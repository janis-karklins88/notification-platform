import { type ChangeEvent, useEffect, useRef, useState } from 'react'

import type { Tenant } from '../tenants/types'
import type { ApiKeyFilter, ApiKeyStatus } from './types'

type ApiKeysFiltersProps = {
  value: ApiKeyFilter
  onChange: (next: Partial<ApiKeyFilter>) => void
  onReset: () => void
  tenantOptions: Tenant[]
  tenantLocked?: boolean
}

type FilterFormState = {
  prefix: string
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

export function ApiKeysFilters({
  value,
  onChange,
  onReset,
  tenantOptions,
  tenantLocked = false,
}: ApiKeysFiltersProps) {
  const [formState, setFormState] = useState<FilterFormState>({
    prefix: value.prefix ?? '',
  })
  const onChangeRef = useRef(onChange)

  useEffect(() => {
    onChangeRef.current = onChange
  }, [onChange])

  useEffect(() => {
    setFormState({ prefix: value.prefix ?? '' })
  }, [value.prefix])

  useEffect(() => {
    const timeoutId = window.setTimeout(() => {
      onChangeRef.current({
        prefix: formState.prefix.trim() || undefined,
      })
    }, 300)

    return () => {
      window.clearTimeout(timeoutId)
    }
  }, [formState.prefix])

  function handleFieldChange(
    event: ChangeEvent<HTMLInputElement | HTMLSelectElement>,
  ) {
    const { name, value: nextValue } = event.target

    if (name === 'prefix') {
      setFormState({ prefix: nextValue })
    }

    if (name === 'tenantId') {
      onChange({ tenantId: nextValue || undefined })
    }

    if (name === 'status') {
      onChange({ status: (nextValue as ApiKeyStatus) || undefined })
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
      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-6">
        <label className="space-y-2">
          <span className="text-sm font-medium text-slate-700">Tenant</span>
          <select
            className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-900 outline-none transition focus:border-slate-500"
            disabled={tenantLocked}
            name="tenantId"
            onChange={handleFieldChange}
            value={value.tenantId ?? ''}
          >
            <option value="">{tenantLocked ? 'Tenant locked' : 'All tenants'}</option>
            {tenantOptions.map((tenant) => (
              <option key={tenant.id} value={tenant.id}>
                {tenant.name}
              </option>
            ))}
          </select>
        </label>

        <label className="space-y-2">
          <span className="text-sm font-medium text-slate-700">Prefix</span>
          <input
            className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-900 outline-none transition focus:border-slate-500"
            name="prefix"
            onChange={handleFieldChange}
            placeholder="np_"
            type="text"
            value={formState.prefix}
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
            <option value="ACTIVE">Active</option>
            <option value="INACTIVE">Inactive</option>
            <option value="REVOKED">Revoked</option>
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

        <label className="space-y-2">
          <span className="text-sm font-medium text-slate-700">Created to</span>
          <input
            className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-900 outline-none transition focus:border-slate-500"
            name="createdTo"
            onChange={handleFieldChange}
            type="datetime-local"
            value={toDateTimeLocalValue(value.createdTo)}
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
