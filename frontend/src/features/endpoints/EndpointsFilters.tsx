import type { ChangeEvent } from 'react'

import type { Tenant } from '../tenants/types'
import type { EndpointFilter, EndpointStatus, EndpointType } from './types'

type EndpointsFiltersProps = {
  value: EndpointFilter
  onChange: (next: Partial<EndpointFilter>) => void
  onReset: () => void
  tenantOptions: Tenant[]
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

export function EndpointsFilters({
  value,
  onChange,
  onReset,
  tenantOptions,
}: EndpointsFiltersProps) {
  function handleFieldChange(
    event: ChangeEvent<HTMLInputElement | HTMLSelectElement>,
  ) {
    const { name, value: nextValue } = event.target

    if (name === 'tenantId') {
      onChange({ tenantId: nextValue || undefined })
    }

    if (name === 'status') {
      onChange({ status: (nextValue as EndpointStatus) || undefined })
    }

    if (name === 'type') {
      onChange({ type: (nextValue as EndpointType) || undefined })
    }

    if (name === 'createdFrom') {
      onChange({ createdFrom: toIsoOrUndefined(nextValue) })
    }

    if (name === 'createdTo') {
      onChange({ createdTo: toIsoOrUndefined(nextValue) })
    }
  }

  return (
    <section className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-5">
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
          <span className="text-sm font-medium text-slate-700">Status</span>
          <select
            className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-900 outline-none transition focus:border-slate-500"
            name="status"
            onChange={handleFieldChange}
            value={value.status ?? ''}
          >
            <option value="">All statuses</option>
            <option value="ACTIVE">Active</option>
            <option value="INACTIVE">Inactive</option>
            <option value="DISABLED">Disabled</option>
          </select>
        </label>

        <label className="space-y-2">
          <span className="text-sm font-medium text-slate-700">Type</span>
          <select
            className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-900 outline-none transition focus:border-slate-500"
            name="type"
            onChange={handleFieldChange}
            value={value.type ?? ''}
          >
            <option value="">All types</option>
            <option value="EMAIL">Email</option>
            <option value="SMS">SMS</option>
            <option value="PUSH_NOTIFICATION">Push notification</option>
            <option value="WEBHOOK">Webhook</option>
          </select>
        </label>

        <label className="space-y-2">
          <span className="text-sm font-medium text-slate-700">Created from</span>
          <input
            className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-900 outline-none transition focus:border-slate-500"
            name="createdFrom"
            onChange={handleFieldChange}
            type="datetime-local"
            value={toDateTimeLocalValue(value.createdFrom)}
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
