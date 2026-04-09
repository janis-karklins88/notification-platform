import { type FormEvent, useEffect, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import {
  createEndpoint,
  updateEndpoint,
} from '../../api/endpointsApi'
import { listEmailTemplates } from '../../api/emailTemplatesApi'
import { notifyCreated, notifyUpdated } from '../../lib/notifications'
import type { EmailTemplate } from '../emailTemplates/types'
import type { Tenant } from '../tenants/types'
import type {
  CreateEndpointRequest,
  EmailEndpointConfig,
  Endpoint,
  EndpointConfig,
  EndpointType,
  UpdateEndpointRequest,
  WebhookEndpointConfig,
} from './types'

type EndpointsFormModalProps = {
  open: boolean
  onClose: () => void
  endpoint?: Endpoint | null
  tenantOptions: Tenant[]
}

type EndpointFormState = {
  tenantId: string
  type: EndpointType
  emailRecipientsText: string
  emailFrom: string
  emailReplyTo: string
  emailTemplateName: string
  emailSubjectTemplate: string
  emailBodyTemplate: string
  emailBodyType: 'text' | 'html'
  webhookUrl: string
  webhookHeadersText: string
  webhookConnectTimeoutMs: string
  webhookResponseTimeoutMs: string
  webhookConnectionRequestTimeoutMs: string
  fallbackConfigText: string
}

const supportedCreateTypes = ['WEBHOOK', 'EMAIL'] as const

const initialFormState: EndpointFormState = {
  tenantId: '',
  type: 'WEBHOOK',
  emailRecipientsText: '',
  emailFrom: '',
  emailReplyTo: '',
  emailTemplateName: '',
  emailSubjectTemplate: '',
  emailBodyTemplate: '',
  emailBodyType: 'html',
  webhookUrl: '',
  webhookHeadersText: '{}',
  webhookConnectTimeoutMs: '',
  webhookResponseTimeoutMs: '',
  webhookConnectionRequestTimeoutMs: '',
  fallbackConfigText: '{}',
}

function isStructuredEndpointType(type: EndpointType) {
  return type === 'EMAIL' || type === 'WEBHOOK'
}

function asObject(value: unknown): Record<string, unknown> {
  return value !== null && typeof value === 'object' && !Array.isArray(value)
    ? (value as Record<string, unknown>)
    : {}
}

function asText(value: unknown) {
  return typeof value === 'string' ? value : ''
}

function asNumberText(value: unknown) {
  return typeof value === 'number' ? String(value) : ''
}

function toRecipientsText(value: unknown) {
  if (!Array.isArray(value)) {
    return ''
  }

  return value
    .filter((recipient): recipient is string => typeof recipient === 'string')
    .join('\n')
}

function toHeadersText(value: unknown) {
  const headers = asObject(value)
  return Object.keys(headers).length > 0 ? JSON.stringify(headers, null, 2) : '{}'
}

function buildStateFromEndpoint(endpoint: Endpoint): EndpointFormState {
  const config = asObject(endpoint.config)

  if (endpoint.type === 'EMAIL') {
    return {
      ...initialFormState,
      tenantId: endpoint.tenantId,
      type: endpoint.type,
      emailRecipientsText: toRecipientsText(config.recipients),
      emailFrom: asText(config.from),
      emailReplyTo: asText(config.replyTo),
      emailTemplateName: asText(config.templateName),
      emailSubjectTemplate: asText(config.subjectTemplate),
      emailBodyTemplate: asText(config.bodyTemplate),
      emailBodyType: asText(config.bodyType) === 'text' ? 'text' : 'html',
      fallbackConfigText: JSON.stringify(endpoint.config, null, 2),
    }
  }

  if (endpoint.type === 'WEBHOOK') {
    return {
      ...initialFormState,
      tenantId: endpoint.tenantId,
      type: endpoint.type,
      webhookUrl: asText(config.url),
      webhookHeadersText: toHeadersText(config.headers),
      webhookConnectTimeoutMs: asNumberText(config.connectTimeoutMs),
      webhookResponseTimeoutMs: asNumberText(config.responseTimeoutMs),
      webhookConnectionRequestTimeoutMs: asNumberText(
        config.connectionRequestTimeoutMs,
      ),
      fallbackConfigText: JSON.stringify(endpoint.config, null, 2),
    }
  }

  return {
    ...initialFormState,
    tenantId: endpoint.tenantId,
    type: endpoint.type,
    fallbackConfigText: JSON.stringify(endpoint.config, null, 2),
  }
}

function toEmailConfig(state: EndpointFormState): EmailEndpointConfig {
  const recipients = state.emailRecipientsText
    .split(/\r?\n|,/)
    .map((value) => value.trim())
    .filter(Boolean)

  if (recipients.length === 0) {
    throw new Error('At least one recipient is required.')
  }

  const config: EmailEndpointConfig = {
    recipients,
    bodyType: state.emailBodyType,
  }

  if (state.emailFrom.trim()) {
    config.from = state.emailFrom.trim()
  }

  if (state.emailReplyTo.trim()) {
    config.replyTo = state.emailReplyTo.trim()
  }

  if (state.emailTemplateName.trim()) {
    config.templateName = state.emailTemplateName.trim()
  }

  if (state.emailSubjectTemplate.trim()) {
    config.subjectTemplate = state.emailSubjectTemplate.trim()
  }

  if (!config.templateName && state.emailBodyTemplate.trim()) {
    config.bodyTemplate = state.emailBodyTemplate.trim()
  }

  return config
}

function parseHeaders(headersText: string) {
  if (!headersText.trim()) {
    return undefined
  }

  const parsed = JSON.parse(headersText) as unknown
  if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
    throw new Error('Webhook headers must be a JSON object.')
  }

  for (const [key, value] of Object.entries(parsed)) {
    if (typeof value !== 'string') {
      throw new Error(`Webhook header "${key}" must have a string value.`)
    }
  }

  return parsed as Record<string, string>
}

function toOptionalInteger(value: string, label: string) {
  if (!value.trim()) {
    return undefined
  }

  const parsed = Number.parseInt(value, 10)
  if (Number.isNaN(parsed)) {
    throw new Error(`${label} must be an integer.`)
  }

  return parsed
}

function toWebhookConfig(state: EndpointFormState): WebhookEndpointConfig {
  const url = state.webhookUrl.trim()
  if (!url) {
    throw new Error('Webhook URL is required.')
  }

  const headers = parseHeaders(state.webhookHeadersText)
  const config: WebhookEndpointConfig = {
    url,
  }

  if (headers && Object.keys(headers).length > 0) {
    config.headers = headers
  }

  const connectTimeoutMs = toOptionalInteger(
    state.webhookConnectTimeoutMs,
    'Connect timeout',
  )
  const responseTimeoutMs = toOptionalInteger(
    state.webhookResponseTimeoutMs,
    'Response timeout',
  )
  const connectionRequestTimeoutMs = toOptionalInteger(
    state.webhookConnectionRequestTimeoutMs,
    'Connection request timeout',
  )

  if (connectTimeoutMs !== undefined) {
    config.connectTimeoutMs = connectTimeoutMs
  }

  if (responseTimeoutMs !== undefined) {
    config.responseTimeoutMs = responseTimeoutMs
  }

  if (connectionRequestTimeoutMs !== undefined) {
    config.connectionRequestTimeoutMs = connectionRequestTimeoutMs
  }

  return config
}

function toFallbackConfig(configText: string): EndpointConfig {
  const parsed = JSON.parse(configText) as unknown

  if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
    throw new Error('Config must be a JSON object.')
  }

  return parsed as EndpointConfig
}

function getEndpointTypeLabel(type: EndpointType) {
  switch (type) {
    case 'EMAIL':
      return 'Email'
    case 'WEBHOOK':
      return 'Webhook'
    case 'SMS':
      return 'SMS'
    case 'PUSH_NOTIFICATION':
      return 'Push notification'
  }
}

function findSelectedTemplate(
  templates: EmailTemplate[] | undefined,
  templateName: string,
) {
  return templates?.find((template) => template.name === templateName)
}

export function EndpointsFormModal({
  open,
  onClose,
  endpoint,
  tenantOptions,
}: EndpointsFormModalProps) {
  const [formState, setFormState] = useState(initialFormState)
  const [error, setError] = useState('')
  const isEditMode = Boolean(endpoint)
  const queryClient = useQueryClient()
  const { data: emailTemplates, isPending: isEmailTemplatesLoading } = useQuery({
    queryKey: ['emailTemplates', formState.tenantId],
    queryFn: () => listEmailTemplates(formState.tenantId),
    enabled: open && Boolean(formState.tenantId),
  })
  const endpointMutation = useMutation({
    mutationFn: async (variables: {
      mode: 'create' | 'edit'
      endpointId?: string
      tenantId?: string
      payload: CreateEndpointRequest | UpdateEndpointRequest
    }) => {
      if (variables.mode === 'edit' && variables.endpointId) {
        return updateEndpoint(
          variables.endpointId,
          variables.payload as UpdateEndpointRequest,
        )
      }

      if (!variables.tenantId) {
        throw new Error('Tenant is required.')
      }

      return createEndpoint(
        variables.tenantId,
        variables.payload as CreateEndpointRequest,
      )
    },
    onSuccess: async (_, variables) => {
      if (variables.mode === 'edit') {
        notifyUpdated('Endpoint')
      } else {
        notifyCreated('Endpoint')
      }

      await queryClient.invalidateQueries({ queryKey: ['endpoints'] })
      onClose()
    },
    onError: (err) => {
      if (err instanceof Error) {
        setError(err.message)
      } else {
        setError('Failed to save endpoint.')
      }
    },
  })

  useEffect(() => {
    if (open) {
      setFormState(
        endpoint
          ? buildStateFromEndpoint(endpoint)
          : {
              ...initialFormState,
              tenantId: tenantOptions[0]?.id ?? '',
            },
      )
      setError('')
      endpointMutation.reset()
    }
  }, [open, endpoint, tenantOptions])

  if (!open) {
    return null
  }

  const selectedTemplate = findSelectedTemplate(
    emailTemplates,
    formState.emailTemplateName,
  )

  function handleClose() {
    if (endpointMutation.isPending) {
      return
    }

    onClose()
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')

    try {
      let config: EndpointConfig

      if (formState.type === 'EMAIL') {
        config = toEmailConfig(formState)
      } else if (formState.type === 'WEBHOOK') {
        config = toWebhookConfig(formState)
      } else {
        config = toFallbackConfig(formState.fallbackConfigText)
      }

      if (isEditMode && endpoint) {
        const payload: UpdateEndpointRequest = { config }

        await endpointMutation.mutateAsync({
          mode: 'edit',
          endpointId: endpoint.id,
          payload,
        })
        return
      }

      if (!formState.tenantId) {
        setError('Tenant is required.')
        return
      }

      const payload: CreateEndpointRequest = {
        type: formState.type,
        config,
      }

      await endpointMutation.mutateAsync({
        mode: 'create',
        payload,
        tenantId: formState.tenantId,
      })
    } catch (err) {
      if (err instanceof Error) {
        setError(err.message)
      } else {
        setError('Failed to save endpoint.')
      }
    }
  }

  const typeOptions = isEditMode && !isStructuredEndpointType(formState.type)
    ? [formState.type, ...supportedCreateTypes]
    : supportedCreateTypes

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/50 px-4 py-6">
      <div className="flex max-h-[calc(100vh-3rem)] w-full max-w-3xl flex-col overflow-hidden rounded-2xl bg-white shadow-2xl">
        <div className="border-b border-slate-200 px-6 py-4">
          <h2 className="text-lg font-semibold text-slate-900">
            {isEditMode ? 'Edit endpoint' : 'Create endpoint'}
          </h2>
          <p className="mt-1 text-sm text-slate-600">
            {isEditMode
              ? 'Update endpoint configuration.'
              : 'Add a new delivery endpoint.'}
          </p>
        </div>

        <form className="flex min-h-0 flex-1 flex-col" onSubmit={handleSubmit}>
          <div className="flex-1 space-y-5 overflow-y-auto px-6 py-5">
            <div className="grid gap-4 md:grid-cols-2">
              <label className="block space-y-2">
                <span className="text-sm font-medium text-slate-700">Tenant</span>
                <select
                  className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-900 outline-none transition focus:border-slate-500 disabled:bg-slate-100"
                  disabled={isEditMode}
                  onChange={(event) =>
                    setFormState((current) => ({
                      ...current,
                      tenantId: event.target.value,
                    }))
                  }
                  value={formState.tenantId}
                >
                  <option value="">Select tenant</option>
                  {tenantOptions.map((tenantOption) => (
                    <option key={tenantOption.id} value={tenantOption.id}>
                      {tenantOption.name}
                    </option>
                  ))}
                </select>
              </label>

              <label className="block space-y-2">
                <span className="text-sm font-medium text-slate-700">Type</span>
                <select
                  className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-900 outline-none transition focus:border-slate-500 disabled:bg-slate-100"
                  disabled={isEditMode}
                  onChange={(event) =>
                    setFormState((current) => ({
                      ...current,
                      type: event.target.value as EndpointType,
                    }))
                  }
                  value={formState.type}
                >
                  {typeOptions.map((typeOption) => (
                    <option key={typeOption} value={typeOption}>
                      {getEndpointTypeLabel(typeOption)}
                    </option>
                  ))}
                </select>
              </label>
            </div>

            {formState.type === 'EMAIL' ? (
              <div className="space-y-5">
                <div className="grid gap-4 md:grid-cols-2">
                  <label className="block space-y-2">
                    <span className="text-sm font-medium text-slate-700">Recipients</span>
                    <textarea
                      className="min-h-28 w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-900 outline-none transition focus:border-slate-500"
                      onChange={(event) =>
                        setFormState((current) => ({
                          ...current,
                          emailRecipientsText: event.target.value,
                        }))
                      }
                      placeholder={'ops@example.com\nsupport@example.com'}
                      value={formState.emailRecipientsText}
                    />
                    <p className="text-xs text-slate-500">
                      Enter one email per line or separate with commas.
                    </p>
                  </label>

                  <div className="grid gap-4">
                    <label className="block space-y-2">
                      <span className="text-sm font-medium text-slate-700">From</span>
                      <input
                        className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-900 outline-none transition focus:border-slate-500"
                        onChange={(event) =>
                          setFormState((current) => ({
                            ...current,
                            emailFrom: event.target.value,
                          }))
                        }
                        placeholder="noreply@example.com"
                        type="email"
                        value={formState.emailFrom}
                      />
                    </label>

                    <label className="block space-y-2">
                      <span className="text-sm font-medium text-slate-700">Reply-to</span>
                      <input
                        className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-900 outline-none transition focus:border-slate-500"
                        onChange={(event) =>
                          setFormState((current) => ({
                            ...current,
                            emailReplyTo: event.target.value,
                          }))
                        }
                        placeholder="support@example.com"
                        type="email"
                        value={formState.emailReplyTo}
                      />
                    </label>
                  </div>
                </div>

                <div className="grid gap-4 md:grid-cols-2">
                  <label className="block space-y-2">
                    <span className="text-sm font-medium text-slate-700">Template</span>
                  <select
                    className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-900 outline-none transition focus:border-slate-500"
                    onChange={(event) =>
                      setFormState((current) => ({
                        ...current,
                          emailTemplateName: event.target.value,
                        }))
                    }
                    value={formState.emailTemplateName}
                  >
                    <option value="">Use custom endpoint template</option>
                    {emailTemplates?.map((template) => (
                      <option key={template.name} value={template.name}>
                        {template.name}
                      </option>
                    ))}
                  </select>
                  <p className="text-xs text-slate-500">
                    {isEmailTemplatesLoading
                      ? 'Loading available backend templates...'
                      : 'Select a reusable backend template, or use a custom endpoint-specific subject and body.'}
                  </p>
                </label>

                  <label className="block space-y-2">
                    <span className="text-sm font-medium text-slate-700">Body type</span>
                    <select
                      className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-900 outline-none transition focus:border-slate-500"
                      onChange={(event) =>
                        setFormState((current) => ({
                          ...current,
                          emailBodyType: event.target.value as 'text' | 'html',
                        }))
                      }
                      value={formState.emailBodyType}
                    >
                      <option value="html">HTML</option>
                      <option value="text">Text</option>
                    </select>
                  </label>
                </div>

                {selectedTemplate ? (
                  <div className="rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-700">
                    <p className="font-medium text-slate-900">{selectedTemplate.name}</p>
                    <p className="mt-1">{selectedTemplate.description}</p>
                    <p className="mt-1 text-xs uppercase tracking-wide text-slate-500">
                      Default body type: {selectedTemplate.html ? 'html' : 'text'}
                    </p>
                  </div>
                ) : null}

                <label className="block space-y-2">
                  <span className="text-sm font-medium text-slate-700">Subject template</span>
                  <input
                    className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-900 outline-none transition focus:border-slate-500 disabled:bg-slate-100"
                    disabled={Boolean(formState.emailTemplateName)}
                    onChange={(event) =>
                      setFormState((current) => ({
                        ...current,
                        emailSubjectTemplate: event.target.value,
                      }))
                    }
                    placeholder="Order {{payload.orderId}} created"
                    type="text"
                    value={formState.emailSubjectTemplate}
                  />
                  <p className="text-xs text-slate-500">
                    {formState.emailTemplateName
                      ? 'Subject is managed by the selected backend template.'
                      : 'Inline subject uses {{payload.*}} placeholders and is stored in endpoint config.'}
                  </p>
                </label>

                <label className="block space-y-2">
                  <span className="text-sm font-medium text-slate-700">Body template</span>
                  <textarea
                    className="min-h-56 w-full rounded-lg border border-slate-300 px-3 py-2 font-mono text-sm text-slate-900 outline-none transition focus:border-slate-500 disabled:bg-slate-100"
                    disabled={Boolean(formState.emailTemplateName)}
                    onChange={(event) =>
                      setFormState((current) => ({
                        ...current,
                        emailBodyTemplate: event.target.value,
                      }))
                    }
                    placeholder={'Hello {{payload.customer.name}},\n\nYour order {{payload.orderId}} was created.'}
                    value={formState.emailBodyTemplate}
                  />
                  <p className="text-xs text-slate-500">
                    {formState.emailTemplateName
                      ? 'Body template is managed by the selected backend template.'
                      : 'Inline body uses {{payload.*}} placeholders and is stored in endpoint config.'}
                  </p>
                </label>
              </div>
            ) : null}

            {formState.type === 'WEBHOOK' ? (
              <div className="space-y-5">
                <label className="block space-y-2">
                  <span className="text-sm font-medium text-slate-700">Webhook URL</span>
                  <input
                    className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-900 outline-none transition focus:border-slate-500"
                    onChange={(event) =>
                      setFormState((current) => ({
                        ...current,
                        webhookUrl: event.target.value,
                      }))
                    }
                    placeholder="https://example.com/webhooks/notifications"
                    type="url"
                    value={formState.webhookUrl}
                  />
                </label>

                <label className="block space-y-2">
                  <span className="text-sm font-medium text-slate-700">Headers JSON</span>
                  <textarea
                    className="min-h-36 w-full rounded-lg border border-slate-300 px-3 py-2 font-mono text-sm text-slate-900 outline-none transition focus:border-slate-500"
                    onChange={(event) =>
                      setFormState((current) => ({
                        ...current,
                        webhookHeadersText: event.target.value,
                      }))
                    }
                    placeholder={'{\n  "X-Api-Key": "secret"\n}'}
                    value={formState.webhookHeadersText}
                  />
                </label>

                <div className="grid gap-4 md:grid-cols-3">
                  <label className="block space-y-2">
                    <span className="text-sm font-medium text-slate-700">
                      Connect timeout (ms)
                    </span>
                    <input
                      className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-900 outline-none transition focus:border-slate-500"
                      onChange={(event) =>
                        setFormState((current) => ({
                          ...current,
                          webhookConnectTimeoutMs: event.target.value,
                        }))
                      }
                      placeholder="2000"
                      type="number"
                      value={formState.webhookConnectTimeoutMs}
                    />
                  </label>

                  <label className="block space-y-2">
                    <span className="text-sm font-medium text-slate-700">
                      Response timeout (ms)
                    </span>
                    <input
                      className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-900 outline-none transition focus:border-slate-500"
                      onChange={(event) =>
                        setFormState((current) => ({
                          ...current,
                          webhookResponseTimeoutMs: event.target.value,
                        }))
                      }
                      placeholder="5000"
                      type="number"
                      value={formState.webhookResponseTimeoutMs}
                    />
                  </label>

                  <label className="block space-y-2">
                    <span className="text-sm font-medium text-slate-700">
                      Request timeout (ms)
                    </span>
                    <input
                      className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-900 outline-none transition focus:border-slate-500"
                      onChange={(event) =>
                        setFormState((current) => ({
                          ...current,
                          webhookConnectionRequestTimeoutMs: event.target.value,
                        }))
                      }
                      placeholder="1000"
                      type="number"
                      value={formState.webhookConnectionRequestTimeoutMs}
                    />
                  </label>
                </div>
              </div>
            ) : null}

            {!isStructuredEndpointType(formState.type) ? (
              <div className="space-y-4">
                <div className="rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800">
                  Structured configuration is currently available only for EMAIL and
                  WEBHOOK endpoints. This endpoint type falls back to raw JSON.
                </div>

                <label className="block space-y-2">
                  <span className="text-sm font-medium text-slate-700">Config JSON</span>
                  <textarea
                    className="min-h-56 w-full rounded-lg border border-slate-300 px-3 py-2 font-mono text-sm text-slate-900 outline-none transition focus:border-slate-500"
                    onChange={(event) =>
                      setFormState((current) => ({
                        ...current,
                        fallbackConfigText: event.target.value,
                      }))
                    }
                    value={formState.fallbackConfigText}
                  />
                </label>
              </div>
            ) : null}

            {error ? (
              <div className="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
                {error}
              </div>
            ) : null}
          </div>

          <div className="flex items-center justify-end gap-3 border-t border-slate-200 px-6 py-4">
            <button
              className="rounded-lg border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 transition hover:border-slate-400 hover:bg-slate-100 disabled:cursor-not-allowed disabled:opacity-60"
              disabled={endpointMutation.isPending}
              onClick={handleClose}
              type="button"
            >
              Cancel
            </button>
            <button
              className="rounded-lg bg-slate-900 px-4 py-2 text-sm font-medium text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-60"
              disabled={endpointMutation.isPending}
              type="submit"
            >
              {endpointMutation.isPending
                ? isEditMode
                  ? 'Saving...'
                  : 'Creating...'
                : isEditMode
                  ? 'Save changes'
                  : 'Create endpoint'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
