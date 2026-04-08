import { type ChangeEvent, useEffect, useRef, useState } from 'react'

import type { Endpoint } from '../endpoints/types'
import type { Tenant } from '../tenants/types'
import type {
  DeliveryChannel,
  DeliveryFilter,
  DeliveryStatus,
} from './types'

type DeliveriesFiltersProps = {
  value: DeliveryFilter
  onChange: (next: Partial<DeliveryFilter>) => void
  onReset: () => void
  tenantOptions: Tenant[]
  endpointOptions: Endpoint[]
}

type FilterFormState = {
  eventId: string
}

const uuidPattern =
  /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i

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

export function DeliveriesFilters({
  value,
  onChange,
  onReset,
  tenantOptions,
  endpointOptions,
}: DeliveriesFiltersProps) {
  const [formState, setFormState] = useState<FilterFormState>({
    eventId: value.eventId ?? '',
  })
  const onChangeRef = useRef(onChange)

  useEffect(() => {
    onChangeRef.current = onChange
  }, [onChange])

  useEffect(() => {
    setFormState({
      eventId: value.eventId ?? '',
    })
  }, [value.eventId])

  useEffect(() => {
    const timeoutId = window.setTimeout(() => {
      const trimmed = formState.eventId.trim()

      if (!trimmed) {
        onChangeRef.current({ eventId: undefined })
        return
      }

      if (uuidPattern.test(trimmed)) {
        onChangeRef.current({ eventId: trimmed })
      }
    }, 300)

    return () => {
      window.clearTimeout(timeoutId)
    }
  }, [formState.eventId])

  function handleFieldChange(
    event: ChangeEvent<HTMLInputElement | HTMLSelectElement>,
  ) {
    const { name, value: nextValue } = event.target

    if (name === 'eventId') {
      setFormState({ eventId: nextValue })
    }

    if (name === 'tenantId') {
      onChange({ tenantId: nextValue || undefined, endpointId: undefined })
    }

    if (name === 'endpointId') {
      onChange({ endpointId: nextValue || undefined })
    }

    if (name === 'status') {
      onChange({ status: (nextValue as DeliveryStatus) || undefined })
    }

    if (name === 'channel') {
      onChange({ channel: (nextValue as DeliveryChannel) || undefined })
    }

    if (name === 'from') {
      onChange({ from: toIsoOrUndefined(nextValue) })
    }

    if (name === 'to') {
      onChange({ to: toIsoOrUndefined(nextValue) })
    }
  }

  return (
    <section className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-7">
        <label className="space-y-2">
          <span className="text-sm font-medium text-slate-700">Tenant</span>
          <select
            className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-900 outline-none transition focus:border-slate-500"
            name="tenantId"
            onChange={handleFieldChange}
            value={value.tenantId ?? ''}
          >
            <option value="">All tenants</option>
            {tenantOptions.map((tenant) => (
              <option key={tenant.id} value={tenant.id}>
                {tenant.name}
              </option>
            ))}
          </select>
        </label>

        <label className="space-y-2">
          <span className="text-sm font-medium text-slate-700">Endpoint</span>
          <select
            className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-900 outline-none transition focus:border-slate-500 disabled:bg-slate-100"
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
          <span className="text-sm font-medium text-slate-700">Event ID</span>
          <input
            className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-900 outline-none transition focus:border-slate-500"
            name="eventId"
            onChange={handleFieldChange}
            placeholder="UUID"
            type="text"
            value={formState.eventId}
          />
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
            <option value="PENDING">Pending</option>
            <option value="IN_PROGRESS">In progress</option>
            <option value="DELIVERED">Delivered</option>
            <option value="FAILED">Failed</option>
          </select>
        </label>

        <label className="space-y-2">
          <span className="text-sm font-medium text-slate-700">Channel</span>
          <select
            className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-900 outline-none transition focus:border-slate-500"
            name="channel"
            onChange={handleFieldChange}
            value={value.channel ?? ''}
          >
            <option value="">All channels</option>
            <option value="EMAIL">Email</option>
            <option value="WEBHOOK">Webhook</option>
            <option value="SMS">SMS</option>
            <option value="PUSH_NOTIFICATION">Push notification</option>
          </select>
        </label>

        <label className="space-y-2">
          <span className="text-sm font-medium text-slate-700">From</span>
          <input
            className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-900 outline-none transition focus:border-slate-500"
            name="from"
            onChange={handleFieldChange}
            type="datetime-local"
            value={toDateTimeLocalValue(value.from)}
          />
        </label>

        <div className="grid gap-4">
          <label className="space-y-2">
            <span className="text-sm font-medium text-slate-700">To</span>
            <input
              className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-900 outline-none transition focus:border-slate-500"
              name="to"
              onChange={handleFieldChange}
              type="datetime-local"
              value={toDateTimeLocalValue(value.to)}
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
      </div>
    </section>
  )
}
