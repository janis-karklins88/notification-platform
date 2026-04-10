import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useParams } from 'react-router-dom'

import { getTenantById, listTenants } from '../api/tenantsApi'
import { listApiKeys, revokeApiKey } from '../api/apiKeysApi'
import { ApiKeysFilters } from '../features/apiKeys/ApiKeysFilters'
import { ApiKeysFormModal } from '../features/apiKeys/ApiKeysFormModal'
import { ApiKeysTable } from '../features/apiKeys/ApiKeysTable'
import type { ApiKey, ApiKeyFilter, CreateApiKeyResponse, PageResponse } from '../features/apiKeys/types'
import { notifySuccess } from '../lib/notifications'

export function ApiKeysPage() {
  const { tenantId: routeTenantId } = useParams()
  const [filters, setFilters] = useState<ApiKeyFilter>({
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
    queryKey: ['apiKeys', filters],
    queryFn: () => listApiKeys(filters),
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

  const revokeMutation = useMutation({
    mutationFn: revokeApiKey,
    onSuccess: async () => {
      notifySuccess('API key revoked successfully.')
      await queryClient.invalidateQueries({ queryKey: ['apiKeys'] })
    },
  })

  function handleFiltersChange(next: Partial<ApiKeyFilter>) {
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

  function handleCreated(createdApiKey: CreateApiKeyResponse) {
    const nextFilters: ApiKeyFilter = {
      ...filters,
      page: 0,
      tenantId: routeTenantId ?? createdApiKey.tenantId,
    }

    const createdListItem: ApiKey = {
      id: createdApiKey.id,
      keyPrefix: createdApiKey.keyPrefix,
      status: createdApiKey.status,
      createdAt: createdApiKey.createdAt,
      revokedAt: null,
      lastUsedAt: null,
    }

    queryClient.setQueryData<PageResponse<ApiKey>>(
      ['apiKeys', nextFilters],
      (current) => {
        if (!current) {
          return {
            items: [createdListItem],
            page: 0,
            size: nextFilters.size ?? 20,
            totalElements: 1,
            totalPages: 1,
            hasNext: false,
            hasPrevious: false,
          }
        }

        const items = [createdListItem, ...current.items.filter((item) => item.id !== createdListItem.id)]
          .slice(0, current.size)
        const totalElements = current.totalElements + 1

        return {
          ...current,
          items,
          page: 0,
          totalElements,
          totalPages: Math.max(1, Math.ceil(totalElements / current.size)),
          hasPrevious: false,
        }
      },
    )

    setFilters(nextFilters)
  }

  function handlePageChange(page: number) {
    setFilters((current) => ({
      ...current,
      page,
    }))
  }

  function handleRevoke(apiKeyId: string) {
    if (!window.confirm('Revoke this API key?')) {
      return
    }

    revokeMutation.mutate(apiKeyId)
  }

  return (
    <section className="space-y-6">
      <header className="flex items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold text-slate-900">
            {tenant ? `${tenant.name} API Keys` : 'API keys'}
          </h1>
          <p className="mt-1 text-sm text-slate-600">
            {routeTenantId
              ? 'Create and manage API keys for the selected tenant.'
              : 'Create and manage tenant API keys.'}
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
            onClick={() => setIsFormModalOpen(true)}
            type="button"
          >
            Create API key
          </button>
        </div>
      </header>

      <ApiKeysFilters
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

      <ApiKeysTable
        apiKeys={data?.items ?? []}
        hasNext={data?.hasNext ?? false}
        hasPrevious={data?.hasPrevious ?? false}
        loading={isPending}
        onPageChange={handlePageChange}
        onRevoke={handleRevoke}
        page={data?.page ?? filters.page ?? 0}
        totalElements={data?.totalElements ?? 0}
        totalPages={data?.totalPages ?? 0}
      />

      <ApiKeysFormModal
        onClose={() => setIsFormModalOpen(false)}
        onCreated={handleCreated}
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
