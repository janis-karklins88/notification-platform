import { type ChangeEvent, useEffect, useRef, useState } from 'react'

import type { Tenant } from '../tenants/types'
import type { Endpoint } from '../endpoints/types'
import type { SubscriptionFilter, SubscriptionStatus } from './types'

type SubscriptionsFiltersProps = {
  value: SubscriptionFilter
  onChange: (next: Partial<SubscriptionFilter>) => void
  onReset: () => void
  tenantOptions: Tenant[]
  endpointOptions: Endpoint[]
}

type FilterFormState = {
  eventType: string
}

function toDateTimeLocalValue(value?: string) {
  if (!value) {
    return ''
  }

  const date = new Date(value)
  const offsetMs = date.getTimezoneOffset() * 60_000
  return new Date(date.getTime() - offsetMs).toISOString().slice(0, 16)
}

function toIsoOrUndefined(value: string) {
  return value ? new Date(value).toISOString() : undefined
}

export function SubscriptionsFilters({
  value,
  onChange,
  onReset,
  tenantOptions,
  endpointOptions,
}: SubscriptionsFiltersProps) {
  const [formState, setFormState] = useState<FilterFormState>({
    eventType: value.eventType ?? '',
  })
  const onChangeRef = useRef(onChange)

  useEffect(() => {
    onChangeRef.current = onChange
  }, [onChange])

  useEffect(() => {
    setFormState({ eventType: value.eventType ?? '' })
  }, [value.eventType])

  useEffect(() => {
    const timeoutId = window.setTimeout(() => {
      onChangeRef.current({
        eventType: formState.eventType.trim() || undefined,
      })
    }, 300)

    return () => {
      window.clearTimeout(timeoutId)
    }
  }, [formState.eventType])

  function handleFieldChange(
    event: ChangeEvent<HTMLInputElement | HTMLSelectElement>,
  ) {
    const { name, value: nextValue } = event.target

    if (name === 'eventType') {
      setFormState({ eventType: nextValue })
    }

    if (name === 'tenantId') {
      onChange({
        tenantId: nextValue || undefined,
        endpointId: undefined,
      })
    }

    if (name === 'endpointId') {
      onChange({ endpointId: nextValue || undefined })
    }

    if (name === 'status') {
      onChange({ status: (nextValue as SubscriptionStatus) || undefined })
    }

    if (name === 'createdAfter') {
      onChange({ createdAfter: toIsoOrUndefined(nextValue) })
    }

    if (name === 'createdBefore') {
      onChange({ createdBefore: toIsoOrUndefined(nextValue) })
    }
  }

  return (
    <section className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-6">
        <label className="space-y-2">
          <span className="text-sm font-medium text-slate-700">Tenant</span>
          <select
            className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-900 outline-none transition focus:border-slate-500"
            name="tenantId"
            onChange={handleFieldChange}
            value={value.tenantId ?? ''}
          >
            <option value="">Select tenant</option>
            {tenantOptions.map((tenant) => (
              <option key={tenant.id} value={tenant.id}>
                {tenant.name}
              </option>
            ))}
          </select>
        </label>

        <label className="space-y-2">
          <span className="text-sm font-medium text-slate-700">Event type</span>
          <input
            className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-900 outline-none transition focus:border-slate-500"
            name="eventType"
            onChange={handleFieldChange}
            placeholder="user.created"
            type="text"
            value={formState.eventType}
          />
        </label>

        <label className="space-y-2">
          <span className="text-sm font-medium text-slate-700">Endpoint</span>
          <select
            className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-900 outline-none transition focus:border-slate-500"
            disabled={!value.tenantId}
            name="endpointId"
            onChange={handleFieldChange}
            value={value.endpointId ?? ''}
          >
            <option value="">All endpoints</option>
            {endpointOptions.map((endpoint) => (
              <option key={endpoint.id} value={endpoint.id}>
                {endpoint.type} · {endpoint.id.slice(0, 8)}
              </option>
            ))}
          </select>
        </label>

        <label className="space-y-2">
          <span className="text-sm font-medium text-slate-700">Status</span>
          <select
            className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-900 outline-none transition focus:border-slate-500"
            name="status"
            onChange={handleFieldChange}
            value={value.status ?? ''}
          >
            <option value="">All statuses</option>
            <option value="ACTIVE">Active</option>
            <option value="PAUSED">Paused</option>
            <option value="DELETED">Deleted</option>
          </select>
        </label>

        <label className="space-y-2">
          <span className="text-sm font-medium text-slate-700">Created from</span>
          <input
            className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-900 outline-none transition focus:border-slate-500"
            name="createdAfter"
            onChange={handleFieldChange}
            type="datetime-local"
            value={toDateTimeLocalValue(value.createdAfter)}
          />
        </label>

        <div className="flex items-end">
          <button
            className="rounded-lg border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 transition hover:border-slate-400 hover:bg-slate-100"
            onClick={onReset}
            type="button"
          >
            Reset
          </button>
        </div>
      </div>
    </section>
  )
}
