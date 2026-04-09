import { type FormEvent, useEffect, useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'

import {
  createEmailTemplate,
  updateEmailTemplate,
} from '../../api/emailTemplatesApi'
import { notifyCreated, notifyUpdated } from '../../lib/notifications'
import type {
  CreateEmailTemplateRequest,
  EmailTemplate,
  UpdateEmailTemplateRequest,
} from './types'

type EmailTemplateFormModalProps = {
  open: boolean
  onClose: () => void
  tenantId: string
  template?: EmailTemplate | null
}

type EmailTemplateFormState = {
  name: string
  subject: string
  body: string
  html: boolean
  description: string
}

const initialFormState: EmailTemplateFormState = {
  name: '',
  subject: '',
  body: '',
  html: true,
  description: '',
}

function buildStateFromTemplate(template: EmailTemplate): EmailTemplateFormState {
  return {
    name: template.name,
    subject: template.subject,
    body: template.body,
    html: template.html,
    description: template.description ?? '',
  }
}

export function EmailTemplateFormModal({
  open,
  onClose,
  tenantId,
  template,
}: EmailTemplateFormModalProps) {
  const [formState, setFormState] = useState(initialFormState)
  const [error, setError] = useState('')
  const isEditMode = Boolean(template)
  const queryClient = useQueryClient()
  const templateMutation = useMutation({
    mutationFn: async () => {
      const trimmedName = formState.name.trim()
      const trimmedSubject = formState.subject.trim()
      const trimmedBody = formState.body.trim()
      const trimmedDescription = formState.description.trim()

      if (!trimmedName) {
        throw new Error('Template name is required.')
      }

      if (!trimmedSubject) {
        throw new Error('Subject is required.')
      }

      if (!trimmedBody) {
        throw new Error('Body is required.')
      }

      if (isEditMode && template) {
        const request: UpdateEmailTemplateRequest = {
          name: trimmedName,
          subject: trimmedSubject,
          body: trimmedBody,
          html: formState.html,
          description: trimmedDescription || undefined,
        }

        return updateEmailTemplate(template.id, request)
      }

      const request: CreateEmailTemplateRequest = {
        name: trimmedName,
        subject: trimmedSubject,
        body: trimmedBody,
        html: formState.html,
        description: trimmedDescription || undefined,
      }

      return createEmailTemplate(tenantId, request)
    },
    onSuccess: async () => {
      if (isEditMode) {
        notifyUpdated('Email template')
      } else {
        notifyCreated('Email template')
      }

      await queryClient.invalidateQueries({ queryKey: ['emailTemplates', tenantId] })
      onClose()
    },
    onError: (err) => {
      if (err instanceof Error) {
        setError(err.message)
      } else {
        setError('Failed to save email template.')
      }
    },
  })

  useEffect(() => {
    if (open) {
      setFormState(template ? buildStateFromTemplate(template) : initialFormState)
      setError('')
      templateMutation.reset()
    }
  }, [open, template])

  if (!open) {
    return null
  }

  function handleClose() {
    if (templateMutation.isPending) {
      return
    }

    onClose()
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')
    await templateMutation.mutateAsync()
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/50 px-4 py-6">
      <div className="w-full max-w-3xl rounded-2xl bg-white shadow-2xl">
        <div className="border-b border-slate-200 px-6 py-4">
          <h2 className="text-lg font-semibold text-slate-900">
            {isEditMode ? 'Edit email template' : 'Create email template'}
          </h2>
          <p className="mt-1 text-sm text-slate-600">
            {isEditMode
              ? 'Update the reusable content for this tenant.'
              : 'Create a reusable tenant email template for endpoint delivery.'}
          </p>
        </div>

        <form className="space-y-5 px-6 py-5" onSubmit={handleSubmit}>
          <div className="grid gap-4 md:grid-cols-2">
            <label className="block space-y-2">
              <span className="text-sm font-medium text-slate-700">Name</span>
              <input
                className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-900 outline-none transition focus:border-slate-500"
                onChange={(event) =>
                  setFormState((current) => ({
                    ...current,
                    name: event.target.value,
                  }))
                }
                placeholder="order-created"
                type="text"
                value={formState.name}
              />
            </label>

            <label className="block space-y-2">
              <span className="text-sm font-medium text-slate-700">Body format</span>
              <select
                className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-900 outline-none transition focus:border-slate-500"
                onChange={(event) =>
                  setFormState((current) => ({
                    ...current,
                    html: event.target.value === 'html',
                  }))
                }
                value={formState.html ? 'html' : 'text'}
              >
                <option value="html">HTML</option>
                <option value="text">Text</option>
              </select>
            </label>
          </div>

          <label className="block space-y-2">
            <span className="text-sm font-medium text-slate-700">Subject</span>
            <input
              className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-900 outline-none transition focus:border-slate-500"
              onChange={(event) =>
                setFormState((current) => ({
                  ...current,
                  subject: event.target.value,
                }))
              }
              placeholder="Order {{payload.orderId}} created"
              type="text"
              value={formState.subject}
            />
          </label>

          <label className="block space-y-2">
            <span className="text-sm font-medium text-slate-700">Description</span>
            <input
              className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-900 outline-none transition focus:border-slate-500"
              onChange={(event) =>
                setFormState((current) => ({
                  ...current,
                  description: event.target.value,
                }))
              }
              placeholder="Used when a new order is created."
              type="text"
              value={formState.description}
            />
          </label>

          <label className="block space-y-2">
            <span className="text-sm font-medium text-slate-700">Body</span>
            <textarea
              className="min-h-72 w-full rounded-lg border border-slate-300 px-3 py-2 font-mono text-sm text-slate-900 outline-none transition focus:border-slate-500"
              onChange={(event) =>
                setFormState((current) => ({
                  ...current,
                  body: event.target.value,
                }))
              }
              placeholder={
                formState.html
                  ? '<h1>Hello {{payload.customerName}}</h1>\n<p>Your order {{payload.orderId}} is ready.</p>'
                  : 'Hello {{payload.customerName}},\n\nYour order {{payload.orderId}} is ready.'
              }
              value={formState.body}
            />
            <p className="text-xs text-slate-500">
              Use <code>{'{{payload.*}}'}</code> placeholders the same way endpoint email
              bodies do.
            </p>
          </label>

          {error ? (
            <div className="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
              {error}
            </div>
          ) : null}

          <div className="flex items-center justify-end gap-3">
            <button
              className="rounded-lg border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 transition hover:border-slate-400 hover:bg-slate-100 disabled:cursor-not-allowed disabled:opacity-60"
              disabled={templateMutation.isPending}
              onClick={handleClose}
              type="button"
            >
              Cancel
            </button>
            <button
              className="rounded-lg bg-slate-900 px-4 py-2 text-sm font-medium text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-60"
              disabled={templateMutation.isPending}
              type="submit"
            >
              {templateMutation.isPending
                ? isEditMode
                  ? 'Saving...'
                  : 'Creating...'
                : isEditMode
                  ? 'Save changes'
                  : 'Create template'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
