import { type FormEvent, useEffect, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { listEndpoints } from '../../api/endpointsApi'
import { createSubscription } from '../../api/subscriptionsApi'
import { notifyCreated } from '../../lib/notifications'
import type { Tenant } from '../tenants/types'

type SubscriptionsFormModalProps = {
  open: boolean
  onClose: () => void
  tenantOptions: Tenant[]
}

type SubscriptionFormState = {
  tenantId: string
  eventType: string
  endpointId: string
}

const initialFormState: SubscriptionFormState = {
  tenantId: '',
  eventType: '',
  endpointId: '',
}

export function SubscriptionsFormModal({
  open,
  onClose,
  tenantOptions,
}: SubscriptionsFormModalProps) {
  const [formState, setFormState] = useState(initialFormState)
  const [error, setError] = useState('')
  const queryClient = useQueryClient()
  const { data: endpointOptions } = useQuery({
    queryKey: ['subscription-form-endpoints', formState.tenantId],
    queryFn: () =>
      listEndpoints({
        page: 0,
        size: 100,
        tenantId: formState.tenantId,
      }),
    enabled: Boolean(formState.tenantId),
  })
  const subscriptionMutation = useMutation({
    mutationFn: async () =>
      createSubscription(formState.tenantId, {
        endpointId: formState.endpointId,
        eventType: formState.eventType.trim(),
      }),
    onSuccess: async () => {
      notifyCreated('Subscription')
      await queryClient.invalidateQueries({ queryKey: ['subscriptions'] })
      onClose()
    },
    onError: (err) => {
      if (err instanceof Error) {
        setError(err.message)
      } else {
        setError('Failed to create subscription.')
      }
    },
  })

  useEffect(() => {
    if (open) {
      setFormState({
        ...initialFormState,
        tenantId: tenantOptions[0]?.id ?? '',
      })
      setError('')
      subscriptionMutation.reset()
    }
  }, [open, tenantOptions])

  useEffect(() => {
    setFormState((current) => ({
      ...current,
      endpointId: '',
    }))
  }, [formState.tenantId])

  if (!open) {
    return null
  }

  function handleClose() {
    if (subscriptionMutation.isPending) {
      return
    }

    onClose()
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    if (!formState.tenantId) {
      setError('Tenant is required.')
      return
    }

    if (!formState.eventType.trim()) {
      setError('Event type is required.')
      return
    }

    if (!formState.endpointId) {
      setError('Endpoint is required.')
      return
    }

    setError('')
    await subscriptionMutation.mutateAsync()
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/50 px-4 py-6">
      <div className="w-full max-w-lg rounded-2xl bg-white shadow-2xl">
        <div className="border-b border-slate-200 px-6 py-4">
          <h2 className="text-lg font-semibold text-slate-900">Create subscription</h2>
          <p className="mt-1 text-sm text-slate-600">
            Connect an event type to a delivery endpoint.
          </p>
        </div>

        <form className="space-y-5 px-6 py-5" onSubmit={handleSubmit}>
          <label className="block space-y-2">
            <span className="text-sm font-medium text-slate-700">Tenant</span>
            <select
              className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-900 outline-none transition focus:border-slate-500"
              onChange={(event) =>
                setFormState((current) => ({
                  ...current,
                  tenantId: event.target.value,
                }))
              }
              value={formState.tenantId}
            >
              <option value="">Select tenant</option>
              {tenantOptions.map((tenant) => (
                <option key={tenant.id} value={tenant.id}>
                  {tenant.name}
                </option>
              ))}
            </select>
          </label>

          <label className="block space-y-2">
            <span className="text-sm font-medium text-slate-700">Event type</span>
            <input
              className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-900 outline-none transition focus:border-slate-500"
              onChange={(event) =>
                setFormState((current) => ({
                  ...current,
                  eventType: event.target.value,
                }))
              }
              placeholder="user.created"
              type="text"
              value={formState.eventType}
            />
          </label>

          <label className="block space-y-2">
            <span className="text-sm font-medium text-slate-700">Endpoint</span>
            <select
              className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-900 outline-none transition focus:border-slate-500"
              disabled={!formState.tenantId}
              onChange={(event) =>
                setFormState((current) => ({
                  ...current,
                  endpointId: event.target.value,
                }))
              }
              value={formState.endpointId}
            >
              <option value="">Select endpoint</option>
              {endpointOptions?.items.map((endpoint) => (
                <option key={endpoint.id} value={endpoint.id}>
                  {endpoint.type} · {endpoint.id.slice(0, 8)}
                </option>
              ))}
            </select>
          </label>

          {error ? (
            <div className="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
              {error}
            </div>
          ) : null}

          <div className="flex items-center justify-end gap-3">
            <button
              className="rounded-lg border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 transition hover:border-slate-400 hover:bg-slate-100 disabled:cursor-not-allowed disabled:opacity-60"
              disabled={subscriptionMutation.isPending}
              onClick={handleClose}
              type="button"
            >
              Cancel
            </button>
            <button
              className="rounded-lg bg-slate-900 px-4 py-2 text-sm font-medium text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-60"
              disabled={subscriptionMutation.isPending}
              type="submit"
            >
              {subscriptionMutation.isPending ? 'Creating...' : 'Create subscription'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
