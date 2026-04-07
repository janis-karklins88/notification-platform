import { type ChangeEvent, useEffect, useRef, useState } from 'react'

import type { TenantFilter, TenantStatus } from './types'

type TenantsFiltersProps = {
  value: TenantFilter
  onChange: (next: Partial<TenantFilter>) => void
  onReset: () => void
}

type FilterFormState = {
  nameContains: string
  status: '' | TenantStatus
  createdFrom: string
  createdTo: string
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

function createInitialState(value: TenantFilter): FilterFormState {
  return {
    nameContains: value.nameContains ?? '',
    status: value.status ?? '',
    createdFrom: toDateTimeLocalValue(value.createdFrom),
    createdTo: toDateTimeLocalValue(value.createdTo),
  }
}

export function TenantsFilters({
  value,
  onChange,
  onReset,
}: TenantsFiltersProps) {
  const [formState, setFormState] = useState<FilterFormState>(() =>
    createInitialState(value),
  )
  const onChangeRef = useRef(onChange)

  useEffect(() => {
    onChangeRef.current = onChange
  }, [onChange])

  useEffect(() => {
    setFormState(createInitialState(value))
  }, [value])

  useEffect(() => {
    const timeoutId = window.setTimeout(() => {
      onChangeRef.current({
        nameContains: formState.nameContains.trim() || undefined,
      })
    }, 500)

    return () => {
      window.clearTimeout(timeoutId)
    }
  }, [formState.nameContains])

  function handleFieldChange(
    event: ChangeEvent<HTMLInputElement | HTMLSelectElement>,
  ) {
    const { name, value: nextValue } = event.target

    setFormState((current) => {
      const nextState = {
        ...current,
        [name]: nextValue,
      }

      if (name === 'status') {
        onChange({
          status: nextState.status || undefined,
        })
      }

      if (name === 'createdFrom') {
        onChange({
          createdFrom: toIsoOrUndefined(nextState.createdFrom),
        })
      }

      if (name === 'createdTo') {
        onChange({
          createdTo: toIsoOrUndefined(nextState.createdTo),
        })
      }

      return nextState
    })
  }

  function handleReset() {
    const emptyState = createInitialState({})
    setFormState(emptyState)
    onReset()
  }

  return (
    <section className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
      <div className="space-y-4">
        <div className="grid gap-4 md:grid-cols-2 xl:xl:grid-cols-[2fr_1fr_1.5fr_1.5fr_auto]">
          <label className="space-y-2">
            <span className="text-sm font-medium text-slate-700">Name</span>
            <input
              className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-900 outline-none transition focus:border-slate-500"
              name="nameContains"
              onChange={handleFieldChange}
              placeholder="Search by tenant name"
              type="text"
              value={formState.nameContains}
            />
          </label>

          <label className="space-y-2">
            <span className="text-sm font-medium text-slate-700">Status</span>
            <select
              className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-900 outline-none transition focus:border-slate-500"
              name="status"
              onChange={handleFieldChange}
              value={formState.status}
            >
              <option value="">All statuses</option>
              <option value="ACTIVE">Active</option>
              <option value="SUSPENDED">Suspended</option>
              <option value="INACTIVE">Inactive</option>
            </select>
          </label>

          <label className="space-y-2">
            <span className="text-sm font-medium text-slate-700">Created from</span>
            <input
              className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-900 outline-none transition focus:border-slate-500"
              name="createdFrom"
              onChange={handleFieldChange}
              type="datetime-local"
              value={formState.createdFrom}
            />
          </label>

          <label className="space-y-2">
            <span className="text-sm font-medium text-slate-700">Created to</span>
            <input
              className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-900 outline-none transition focus:border-slate-500"
              name="createdTo"
              onChange={handleFieldChange}
              type="datetime-local"
              value={formState.createdTo}
            />
          </label>
          <div className="flex items-end">
          <button
            className="rounded-lg border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 transition hover:border-slate-400 hover:bg-slate-100"
            onClick={handleReset}
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
