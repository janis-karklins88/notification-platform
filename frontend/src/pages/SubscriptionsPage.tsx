import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useParams } from 'react-router-dom'

import { listEndpoints } from '../api/endpointsApi'
import {
  deactivateSubscription,
  deleteSubscription,
  listSubscriptions,
  reactivateSubscription,
} from '../api/subscriptionsApi'
import { getTenantById, listTenants } from '../api/tenantsApi'
import { SubscriptionsFilters } from '../features/subscriptions/SubscriptionsFilters'
import { SubscriptionsFormModal } from '../features/subscriptions/SubscriptionsFormModal'
import { SubscriptionsTable } from '../features/subscriptions/SubscriptionsTable'
import type { SubscriptionFilter } from '../features/subscriptions/types'
import { notifyDeleted, notifySuccess, notifyUpdated } from '../lib/notifications'

export function SubscriptionsPage() {
  const { tenantId: routeTenantId } = useParams()
  const [filters, setFilters] = useState<SubscriptionFilter>({
    page: 0,
    size: 20,
    tenantId: routeTenantId,
  })
  const [isFormModalOpen, setIsFormModalOpen] = useState(false)
  const queryClient = useQueryClient()

  const {
    data,
    error,
    isPending,
    refetch,
  } = useQuery({
    queryKey: ['subscriptions', filters],
    queryFn: () => listSubscriptions(filters),
    enabled: Boolean(filters.tenantId),
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
    queryKey: ['subscription-filter-endpoints', filters.tenantId],
    queryFn: () =>
      listEndpoints({
        page: 0,
        size: 100,
        tenantId: filters.tenantId,
      }),
    enabled: Boolean(filters.tenantId),
  })

  const subscriptionActionMutation = useMutation({
    mutationFn: async (variables: {
      action: 'deactivate' | 'reactivate' | 'delete'
      subscriptionId: string
    }) => {
      if (variables.action === 'deactivate') {
        return deactivateSubscription(variables.subscriptionId)
      }

      if (variables.action === 'reactivate') {
        return reactivateSubscription(variables.subscriptionId)
      }

      return deleteSubscription(variables.subscriptionId)
    },
    onSuccess: async (_, variables) => {
      if (variables.action === 'deactivate') {
        notifyUpdated('Subscription')
      }

      if (variables.action === 'reactivate') {
        notifySuccess('Subscription reactivated successfully.')
      }

      if (variables.action === 'delete') {
        notifyDeleted('Subscription')
      }

      await queryClient.invalidateQueries({ queryKey: ['subscriptions'] })
    },
  })

  function handleCreateOpen() {
    setIsFormModalOpen(true)
  }

  function handleFormClose() {
    setIsFormModalOpen(false)
  }

  function handleFiltersChange(next: Partial<SubscriptionFilter>) {
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
    if (filters.tenantId) {
      refetch()
    }
  }

  function handlePageChange(page: number) {
    setFilters((current) => ({
      ...current,
      page,
    }))
  }

  function handleDelete(subscriptionId: string) {
    if (!window.confirm('Delete this subscription?')) {
      return
    }

    subscriptionActionMutation.mutate({
      action: 'delete',
      subscriptionId,
    })
  }

  return (
    <section className="space-y-6">
      <header className="flex items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold text-slate-900">
            {tenant ? `${tenant.name} Subscriptions` : 'Subscriptions'}
          </h1>
          <p className="mt-1 text-sm text-slate-600">
            {routeTenantId
              ? 'Manage event routing subscriptions for the selected tenant.'
              : 'Manage event routing subscriptions by tenant.'}
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
            Create subscription
          </button>
        </div>
      </header>

      <SubscriptionsFilters
        endpointOptions={endpointOptions?.items ?? []}
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

      <SubscriptionsTable
        emptyMessage={
          filters.tenantId
            ? 'No subscriptions found.'
            : 'Select a tenant to view subscriptions.'
        }
        hasNext={data?.hasNext ?? false}
        hasPrevious={data?.hasPrevious ?? false}
        loading={Boolean(filters.tenantId) && isPending}
        onDeactivate={(subscriptionId) =>
          subscriptionActionMutation.mutate({
            action: 'deactivate',
            subscriptionId,
          })
        }
        onDelete={(subscriptionId) =>
          handleDelete(subscriptionId)
        }
        onPageChange={handlePageChange}
        onReactivate={(subscriptionId) =>
          subscriptionActionMutation.mutate({
            action: 'reactivate',
            subscriptionId,
          })
        }
        page={data?.page ?? filters.page ?? 0}
        showEmptyState={!filters.tenantId || (data?.items?.length ?? 0) === 0}
        subscriptions={data?.items ?? []}
        totalElements={data?.totalElements ?? 0}
        totalPages={data?.totalPages ?? 0}
      />

      <SubscriptionsFormModal
        onClose={handleFormClose}
        open={isFormModalOpen}
        tenantOptions={
          routeTenantId
            ? (tenantOptions?.items ?? []).filter((tenantOption) => tenantOption.id === routeTenantId)
            : tenantOptions?.items ?? []
        }
      />
    </section>
  )
}
