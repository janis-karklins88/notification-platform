import { type FormEvent, useEffect, useState } from 'react'

import { createTenant, editTenant } from '../../api/tenantsApi'
import { notifyCreated, notifyUpdated } from '../../lib/notifications'
import type {
  CreateTenantRequest,
  EditTenantRequest,
  Tenant,
  TenantStatus,
} from './types'

type TenantsFormModalProps = {
  open: boolean
  onClose: () => void
  onSuccess: () => void
  tenant?: Tenant | null
}

type TenantFormState = {
  slug: string
  name: string
  status: TenantStatus
}

const initialFormState: TenantFormState = {
  slug: '',
  name: '',
  status: 'ACTIVE',
}

export function TenantsFormModal({
  open,
  onClose,
  onSuccess,
  tenant,
}: TenantsFormModalProps) {
  const [formState, setFormState] = useState<TenantFormState>(initialFormState)
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const isEditMode = Boolean(tenant)

  useEffect(() => {
    if (open) {
      setFormState(
        tenant
          ? {
              slug: tenant.slug,
              name: tenant.name,
              status: tenant.status,
            }
          : initialFormState,
      )
      setError('')
      setSubmitting(false)
    }
  }, [open, tenant])

  if (!open) {
    return null
  }

  function handleClose() {
    if (submitting) {
      return
    }

    onClose()
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    const trimmedName = formState.name.trim()
    const trimmedSlug = formState.slug.trim()

    if (!trimmedName) {
      setError('Name is required.')
      return
    }

    if (!isEditMode && !trimmedSlug) {
      setError('Slug is required.')
      return
    }

    try {
      setSubmitting(true)
      setError('')

      if (isEditMode && tenant) {
        const payload: EditTenantRequest = {
          name: trimmedName,
          status: formState.status,
        }
        await editTenant(tenant.id, payload)
        notifyUpdated('Tenant')
      } else {
        const payload: CreateTenantRequest = {
          slug: trimmedSlug,
          name: trimmedName,
          status: formState.status,
        }
        await createTenant(payload)
        notifyCreated('Tenant')
      }

      onSuccess()
      onClose()
    } catch (err) {
      if (err instanceof Error) {
        setError(err.message)
      } else {
        setError('Failed to create tenant.')
      }
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/50 px-4 py-6">
      <div className="w-full max-w-lg rounded-2xl bg-white shadow-2xl">
        <div className="border-b border-slate-200 px-6 py-4">
          <h2 className="text-lg font-semibold text-slate-900">
            {isEditMode ? 'Edit tenant' : 'Create tenant'}
          </h2>
          <p className="mt-1 text-sm text-slate-600">
            {isEditMode
              ? 'Update the selected tenant details.'
              : 'Add a new tenant to the notification platform.'}
          </p>
        </div>

        <form className="space-y-5 px-6 py-5" onSubmit={handleSubmit}>
          <label className="block space-y-2">
            <span className="text-sm font-medium text-slate-700">Slug</span>
            <input
              className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-900 outline-none transition focus:border-slate-500"
              onChange={(event) =>
                setFormState((current) => ({
                  ...current,
                  slug: event.target.value,
                }))
              }
              disabled={isEditMode}
              placeholder="slug"
              type="text"
              value={formState.slug}
            />
          </label>

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
              placeholder="name"
              type="text"
              value={formState.name}
            />
          </label>

          <label className="block space-y-2">
            <span className="text-sm font-medium text-slate-700">Status</span>
            <select
              className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-900 outline-none transition focus:border-slate-500"
              onChange={(event) =>
                setFormState((current) => ({
                  ...current,
                  status: event.target.value as TenantStatus,
                }))
              }
              value={formState.status}
            >
              <option value="ACTIVE">Active</option>
              <option value="SUSPENDED">Suspended</option>
              <option value="INACTIVE">Inactive</option>
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
              onClick={handleClose}
              type="button"
            >
              Cancel
            </button>
            <button
              className="rounded-lg bg-slate-900 px-4 py-2 text-sm font-medium text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-60"
              disabled={submitting}
              type="submit"
            >
              {submitting
                ? isEditMode
                  ? 'Saving...'
                  : 'Creating...'
                : isEditMode
                  ? 'Save changes'
                  : 'Create tenant'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
