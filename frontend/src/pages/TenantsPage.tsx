import { useEffect, useState } from 'react'

import { listTenants } from '../api/tenantsApi'
import { TenantsFilters } from '../features/tenants/TenantsFilters'
import { TenantsFormModal } from '../features/tenants/TenantsFormModal'
import { TenantsTable } from '../features/tenants/TenantsTable'
import {
  type PageResponse,
  type Tenant,
  type TenantFilter,
} from '../features/tenants/types'

export function TenantsPage() {
  const [filters, setFilters] = useState<TenantFilter>({ page: 0, size: 20 })
  const [data, setData] = useState<PageResponse<Tenant> | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [isFormModalOpen, setIsFormModalOpen] = useState(false)
  const [selectedTenant, setSelectedTenant] = useState<Tenant | null>(null)
  const [refreshNonce, setRefreshNonce] = useState(0)

  useEffect(() => {
    async function loadTenants() {
      try {
        setLoading(true)
        setError('')
        const response = await listTenants(filters)
        setData(response)
      } catch (err) {
        if (err instanceof Error) {
      setError(err.message)
      } else {
      setError('Failed to load tenants')
      }
      } finally {
        setLoading(false)
      }
    }

    loadTenants()
  }, [filters, refreshNonce])

  function handleEdit(tenant: Tenant) {
    setSelectedTenant(tenant)
    setIsFormModalOpen(true)
  }

  function handleCreateOpen() {
    setSelectedTenant(null)
    setIsFormModalOpen(true)
  }

  function handleFormClose() {
    setIsFormModalOpen(false)
    setSelectedTenant(null)
  }

  function handleFormSuccess() {
    setRefreshNonce((current) => current + 1)
  }

  function handleFiltersChange(next: Partial<TenantFilter>) {
    setFilters((current) => ({
      ...current,
      ...next,
      page: 0,
    }))
  }

  function handleResetFilters() {
    setFilters({ page: 0, size: 20 })
  }

  function handleRefresh() {
    setRefreshNonce((current) => current + 1)
  }

  function handlePageChange(page: number) {
    setFilters((current) => ({
      ...current,
      page,
    }))
  }

  return (
    <section className="space-y-6">
      <header className="flex items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold text-slate-900">Tenants</h1>
          <p className="mt-1 text-sm text-slate-600">Manage platform tenants.</p>
        </div>
        <div className="flex items-center gap-3">
          <button
            className="rounded-lg border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 transition hover:border-slate-400 hover:bg-slate-100"
            onClick={handleRefresh}
            type="button"
          >
            Refresh
          </button>
          <button
            className="rounded-lg bg-slate-900 px-4 py-2 text-sm font-medium text-white transition hover:bg-slate-800"
            onClick={handleCreateOpen}
            type="button"
          >
            Create tenant
          </button>
        </div>
      </header>

      <TenantsFilters
        onChange={handleFiltersChange}
        onReset={handleResetFilters}
        value={filters}
      />

      {error ? (
        <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {error}
        </div>
      ) : null}

      <TenantsTable
        hasNext={data?.hasNext ?? false}
        hasPrevious={data?.hasPrevious ?? false}
        onPageChange={handlePageChange}
        tenants={data?.items ?? []}
        loading={loading}
        onEdit={handleEdit}
        page={data?.page ?? filters.page ?? 0}
        totalElements={data?.totalElements ?? 0}
        totalPages={data?.totalPages ?? 0}
      />

      <TenantsFormModal
        onClose={handleFormClose}
        onSuccess={handleFormSuccess}
        open={isFormModalOpen}
        tenant={selectedTenant}
      />
    </section>
  )
}
