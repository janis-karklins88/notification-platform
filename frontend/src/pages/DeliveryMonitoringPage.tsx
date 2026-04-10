import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { useParams } from 'react-router-dom'

import { listDeliveries } from '../api/deliveriesApi'
import { listEndpoints } from '../api/endpointsApi'
import { getTenantById, listTenants } from '../api/tenantsApi'
import { DeliveriesFilters } from '../features/deliveries/DeliveriesFilters'
import { DeliveryDetailsModal } from '../features/deliveries/DeliveryDetailsModal'
import { DeliveriesTable } from '../features/deliveries/DeliveriesTable'
import type { Delivery, DeliveryFilter } from '../features/deliveries/types'

export function DeliveryMonitoringPage() {
  const { tenantId: routeTenantId } = useParams()
  const [filters, setFilters] = useState<DeliveryFilter>({
    page: 0,
    size: 20,
    tenantId: routeTenantId,
  })
  const [selectedDelivery, setSelectedDelivery] = useState<Delivery | null>(null)

  const {
    data,
    error,
    isPending,
    refetch,
  } = useQuery({
    queryKey: ['deliveries', filters],
    queryFn: () => listDeliveries(filters),
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

  const { data: endpointOptions } = useQuery({
    queryKey: ['delivery-filter-endpoints', filters.tenantId],
    queryFn: () =>
      listEndpoints({
        page: 0,
        size: 100,
        tenantId: filters.tenantId,
      }),
    enabled: Boolean(filters.tenantId),
  })

  const tenantNamesById = Object.fromEntries(
    (tenantOptions?.items ?? []).map((tenant) => [tenant.id, tenant.name]),
  )

  function handleFiltersChange(next: Partial<DeliveryFilter>) {
    setFilters((current) => ({
      ...current,
      ...next,
      tenantId: routeTenantId ?? next.tenantId ?? current.tenantId,
      page: 0,
    }))
  }

  function handleResetFilters() {
    setFilters({ page: 0, size: 20, tenantId: routeTenantId })
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

  return (
    <section className="space-y-6">
      <header className="flex items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold text-slate-900">
            {tenant ? `${tenant.name} Deliveries` : 'Deliveries'}
          </h1>
          <p className="mt-1 text-sm text-slate-600">
            {routeTenantId
              ? 'Monitor delivery attempts for the selected tenant.'
              : 'Monitor delivery attempts across channels and tenants.'}
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
        </div>
      </header>

      <DeliveriesFilters
        endpointOptions={endpointOptions?.items ?? []}
        onChange={handleFiltersChange}
        onReset={handleResetFilters}
        tenantLocked={Boolean(routeTenantId)}
        tenantOptions={tenantOptions?.items ?? []}
        value={filters}
      />

      {error instanceof Error ? (
        <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {error.message}
        </div>
      ) : null}

      <DeliveriesTable
        deliveries={data?.items ?? []}
        hasNext={data?.hasNext ?? false}
        hasPrevious={data?.hasPrevious ?? false}
        loading={isPending}
        onPageChange={handlePageChange}
        onViewDetails={setSelectedDelivery}
        page={data?.page ?? filters.page ?? 0}
        showTenantColumn={!routeTenantId}
        tenantNamesById={tenantNamesById}
        totalElements={data?.totalElements ?? 0}
        totalPages={data?.totalPages ?? 0}
      />

      <DeliveryDetailsModal
        deliveryId={selectedDelivery?.id ?? null}
        onClose={() => setSelectedDelivery(null)}
        open={Boolean(selectedDelivery)}
        tenantName={
          selectedDelivery
            ? tenantNamesById[selectedDelivery.tenantId] ?? selectedDelivery.tenantId
            : undefined
        }
      />
    </section>
  )
}
