import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useParams } from 'react-router-dom'

import {
  deactivateEndpoint,
  deleteEndpoint,
  listEndpoints,
  reactivateEndpoint,
} from '../api/endpointsApi'
import { getTenantById, listTenants } from '../api/tenantsApi'
import { EndpointDetailsModal } from '../features/endpoints/EndpointDetailsModal'
import { EndpointsFilters } from '../features/endpoints/EndpointsFilters'
import { EndpointsFormModal } from '../features/endpoints/EndpointsFormModal'
import { EndpointsTable } from '../features/endpoints/EndpointsTable'
import type { Endpoint, EndpointFilter } from '../features/endpoints/types'
import { notifyDeleted, notifySuccess, notifyUpdated } from '../lib/notifications'

export function EndpointsPage() {
  const { tenantId: routeTenantId } = useParams()
  const [filters, setFilters] = useState<EndpointFilter>({
    page: 0,
    size: 20,
    tenantId: routeTenantId,
  })
  const [isFormModalOpen, setIsFormModalOpen] = useState(false)
  const [selectedEndpoint, setSelectedEndpoint] = useState<Endpoint | null>(null)
  const [detailsEndpoint, setDetailsEndpoint] = useState<Endpoint | null>(null)
  const queryClient = useQueryClient()

  const {
    data,
    error,
    isPending,
    refetch,
  } = useQuery({
    queryKey: ['endpoints', filters],
    queryFn: () => listEndpoints(filters),
  })

  const { data: tenantOptions } = useQuery({
    queryKey: ['tenants', 'options'],
    queryFn: () => listTenants({ page: 0, size: 100 }),
  })

  const { data: tenant } = useQuery({
    queryKey: ['tenant', routeTenantId],
    queryFn: () => getTenantById(routeTenantId!),
    enabled: Boolean(routeTenantId),
  })

  const tenantNamesById = Object.fromEntries(
    (tenantOptions?.items ?? []).map((tenant) => [tenant.id, tenant.name]),
  )

  const endpointActionMutation = useMutation({
    mutationFn: async (variables: {
      action: 'deactivate' | 'reactivate' | 'delete'
      endpointId: string
    }) => {
      if (variables.action === 'deactivate') {
        return deactivateEndpoint(variables.endpointId)
      }

      if (variables.action === 'reactivate') {
        return reactivateEndpoint(variables.endpointId)
      }

      return deleteEndpoint(variables.endpointId)
    },
    onSuccess: async (_, variables) => {
      if (variables.action === 'deactivate') {
        notifyUpdated('Endpoint')
      }

      if (variables.action === 'reactivate') {
        notifySuccess('Endpoint reactivated successfully.')
      }

      if (variables.action === 'delete') {
        notifyDeleted('Endpoint')
      }

      await queryClient.invalidateQueries({ queryKey: ['endpoints'] })
    },
  })

  function handleCreateOpen() {
    setSelectedEndpoint(null)
    setIsFormModalOpen(true)
  }

  function handleEdit(endpoint: Endpoint) {
    setSelectedEndpoint(endpoint)
    setIsFormModalOpen(true)
  }

  function handleFormClose() {
    setIsFormModalOpen(false)
    setSelectedEndpoint(null)
  }

  function handleDetailsOpen(endpoint: Endpoint) {
    setDetailsEndpoint(endpoint)
  }

  function handleDetailsClose() {
    setDetailsEndpoint(null)
  }

  function handleFiltersChange(next: Partial<EndpointFilter>) {
    setFilters((current) => ({
      ...current,
      ...next,
      tenantId: routeTenantId ?? next.tenantId ?? current.tenantId,
      page: 0,
    }))
  }

  function handleResetFilters() {
    setFilters({
      page: 0,
      size: 20,
      tenantId: routeTenantId,
    })
  }

  function handleRefresh() {
    refetch()
  }

  function handlePageChange(page: number) {
    setFilters((current) => ({
      ...current,
      page,
    }))
  }

  function handleDeactivate(endpointId: string) {
    endpointActionMutation.mutate({ action: 'deactivate', endpointId })
  }

  function handleReactivate(endpointId: string) {
    endpointActionMutation.mutate({ action: 'reactivate', endpointId })
  }

  function handleDelete(endpointId: string) {
    if (!window.confirm('Delete this endpoint?')) {
      return
    }

    endpointActionMutation.mutate({ action: 'delete', endpointId })
  }

  return (
    <section className="space-y-6">
      <header className="flex items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold text-slate-900">
            {tenant ? `${tenant.name} Endpoints` : 'Endpoints'}
          </h1>
          <p className="mt-1 text-sm text-slate-600">
            {routeTenantId
              ? 'Manage delivery endpoints for the selected tenant.'
              : 'Manage delivery endpoints and their lifecycle.'}
          </p>
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
            Create endpoint
          </button>
        </div>
      </header>

      <EndpointsFilters
        onChange={handleFiltersChange}
        onReset={handleResetFilters}
        tenantLocked={Boolean(routeTenantId)}
        tenantOptions={
          routeTenantId
            ? (tenantOptions?.items ?? []).filter((tenantOption) => tenantOption.id === routeTenantId)
            : tenantOptions?.items ?? []
        }
        value={filters}
      />

      {error instanceof Error ? (
        <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {error.message}
        </div>
      ) : null}

      <EndpointsTable
        endpoints={data?.items ?? []}
        hasNext={data?.hasNext ?? false}
        hasPrevious={data?.hasPrevious ?? false}
        loading={isPending}
        onDeactivate={handleDeactivate}
        onDelete={handleDelete}
        onEdit={handleEdit}
        onPageChange={handlePageChange}
        onReactivate={handleReactivate}
        onViewDetails={handleDetailsOpen}
        page={data?.page ?? filters.page ?? 0}
        tenantNamesById={tenantNamesById}
        totalElements={data?.totalElements ?? 0}
        totalPages={data?.totalPages ?? 0}
      />

      <EndpointsFormModal
        endpoint={selectedEndpoint}
        onClose={handleFormClose}
        open={isFormModalOpen}
        tenantOptions={
          routeTenantId
            ? (tenantOptions?.items ?? []).filter((tenantOption) => tenantOption.id === routeTenantId)
            : tenantOptions?.items ?? []
        }
      />

      <EndpointDetailsModal
        endpoint={detailsEndpoint}
        onClose={handleDetailsClose}
        open={Boolean(detailsEndpoint)}
        tenantName={
          detailsEndpoint
            ? tenantNamesById[detailsEndpoint.tenantId] ?? detailsEndpoint.tenantId
            : undefined
        }
      />
    </section>
  )
}
