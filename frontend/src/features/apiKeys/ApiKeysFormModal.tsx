import { useEffect, useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'

import { createApiKey } from '../../api/apiKeysApi'
import { notifyCreated } from '../../lib/notifications'
import type { Tenant } from '../tenants/types'
import type { CreateApiKeyResponse } from './types'

type ApiKeysFormModalProps = {
  open: boolean
  onClose: () => void
  tenantOptions: Tenant[]
}

export function ApiKeysFormModal({
  open,
  onClose,
  tenantOptions,
}: ApiKeysFormModalProps) {
  const [tenantId, setTenantId] = useState('')
  const [error, setError] = useState('')
  const [createdApiKey, setCreatedApiKey] = useState<CreateApiKeyResponse | null>(null)
  const queryClient = useQueryClient()
  const createApiKeyMutation = useMutation({
    mutationFn: async () => createApiKey(tenantId),
    onSuccess: async (response) => {
      setCreatedApiKey(response)
      notifyCreated('API key')
      await queryClient.invalidateQueries({ queryKey: ['apiKeys'] })
    },
    onError: (err) => {
      if (err instanceof Error) {
        setError(err.message)
      } else {
        setError('Failed to create API key.')
      }
    },
  })

  useEffect(() => {
    if (open) {
      setTenantId(tenantOptions[0]?.id ?? '')
      setError('')
      setCreatedApiKey(null)
      createApiKeyMutation.reset()
    }
  }, [open, tenantOptions, createApiKeyMutation])

  if (!open) {
    return null
  }

  function handleClose() {
    if (createApiKeyMutation.isPending) {
      return
    }

    onClose()
  }

  async function handleSubmit() {
    if (!tenantId) {
      setError('Tenant is required.')
      return
    }

    setError('')
    await createApiKeyMutation.mutateAsync()
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/50 px-4 py-6">
      <div className="w-full max-w-lg rounded-2xl bg-white shadow-2xl">
        <div className="border-b border-slate-200 px-6 py-4">
          <h2 className="text-lg font-semibold text-slate-900">Create API key</h2>
          <p className="mt-1 text-sm text-slate-600">
            Generate a new API key for a tenant.
          </p>
        </div>

        <div className="space-y-5 px-6 py-5">
          {createdApiKey ? (
            <div className="space-y-4">
              <div className="rounded-lg border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-800">
                Copy this plaintext key now. It will not be shown again.
              </div>
              <div className="space-y-2">
                <p className="text-sm font-medium text-slate-700">Key prefix</p>
                <div className="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 font-mono text-sm text-slate-900">
                  {createdApiKey.keyPrefix}
                </div>
              </div>
              <div className="space-y-2">
                <p className="text-sm font-medium text-slate-700">Plaintext key</p>
                <div className="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 font-mono text-sm text-slate-900">
                  {createdApiKey.plaintextKey}
                </div>
              </div>
            </div>
          ) : (
            <label className="block space-y-2">
              <span className="text-sm font-medium text-slate-700">Tenant</span>
              <select
                className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-900 outline-none transition focus:border-slate-500"
                onChange={(event) => setTenantId(event.target.value)}
                value={tenantId}
              >
                <option value="">Select tenant</option>
                {tenantOptions.map((tenant) => (
                  <option key={tenant.id} value={tenant.id}>
                    {tenant.name}
                  </option>
                ))}
              </select>
            </label>
          )}

          {error ? (
            <div className="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
              {error}
            </div>
          ) : null}

          <div className="flex items-center justify-end gap-3">
            <button
              className="rounded-lg border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 transition hover:border-slate-400 hover:bg-slate-100 disabled:cursor-not-allowed disabled:opacity-60"
              disabled={createApiKeyMutation.isPending}
              onClick={handleClose}
              type="button"
            >
              {createdApiKey ? 'Close' : 'Cancel'}
            </button>
            {!createdApiKey ? (
              <button
                className="rounded-lg bg-slate-900 px-4 py-2 text-sm font-medium text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-60"
                disabled={createApiKeyMutation.isPending}
                onClick={handleSubmit}
                type="button"
              >
                {createApiKeyMutation.isPending ? 'Creating...' : 'Create API key'}
              </button>
            ) : null}
          </div>
        </div>
      </div>
    </div>
  )
}
