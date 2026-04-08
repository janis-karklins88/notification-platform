import { type FormEvent, useEffect, useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'

import { createEndpoint, updateEndpoint } from '../../api/endpointsApi'
import { notifyCreated, notifyUpdated } from '../../lib/notifications'
import type { Tenant } from '../tenants/types'
import type {
  CreateEndpointRequest,
  Endpoint,
  EndpointType,
  UpdateEndpointRequest,
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
  configText: string
}

const initialFormState: EndpointFormState = {
  tenantId: '',
  type: 'WEBHOOK',
  configText: '{}',
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
  const endpointMutation = useMutation({
    mutationFn: async (variables: {
      mode: 'create' | 'edit'
      endpointId?: string
      tenantId?: string
      payload: CreateEndpointRequest | UpdateEndpointRequest
    }) => {
      if (variables.mode === 'edit' && variables.endpointId) {
        return updateEndpoint(variables.endpointId, variables.payload as UpdateEndpointRequest)
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
          ? {
              tenantId: endpoint.tenantId,
              type: endpoint.type,
              configText: JSON.stringify(endpoint.config, null, 2),
            }
          : {
              ...initialFormState,
              tenantId: tenantOptions[0]?.id ?? '',
            },
      )
      setError('')
      endpointMutation.reset()
    }
  }, [open, endpoint, tenantOptions, endpointMutation])

  if (!open) {
    return null
  }

  function handleClose() {
    if (endpointMutation.isPending) {
      return
    }

    onClose()
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    try {
      setError('')
      const parsedConfig = JSON.parse(formState.configText) as Record<string, unknown>

      if (isEditMode && endpoint) {
        const payload: UpdateEndpointRequest = {
          config: parsedConfig,
        }

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
        config: parsedConfig,
      }

      await endpointMutation.mutateAsync({
        mode: 'create',
        payload,
        tenantId: formState.tenantId,
      })
    } catch (err) {
      if (err instanceof SyntaxError) {
        setError('Config must be valid JSON.')
      }
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/50 px-4 py-6">
      <div className="w-full max-w-2xl rounded-2xl bg-white shadow-2xl">
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

        <form className="space-y-5 px-6 py-5" onSubmit={handleSubmit}>
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
                <option value="WEBHOOK">Webhook</option>
                <option value="EMAIL">Email</option>
                <option value="SMS">SMS</option>
                <option value="PUSH_NOTIFICATION">Push notification</option>
              </select>
            </label>
          </div>

          <label className="block space-y-2">
            <span className="text-sm font-medium text-slate-700">Config JSON</span>
            <textarea
              className="min-h-56 w-full rounded-lg border border-slate-300 px-3 py-2 font-mono text-sm text-slate-900 outline-none transition focus:border-slate-500"
              onChange={(event) =>
                setFormState((current) => ({
                  ...current,
                  configText: event.target.value,
                }))
              }
              value={formState.configText}
            />
          </label>

          {error ? (
            <div className="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
              {error}
            </div>
          ) : null}

          <div className="flex items-center justify-end gap-3">
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
