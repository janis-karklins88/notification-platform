import { apiFetch } from './client'

import type {
  ApiKey,
  ApiKeyFilter,
  CreateApiKeyResponse,
  PageResponse,
} from '../features/apiKeys/types'

export async function listApiKeys(
  filter: ApiKeyFilter,
): Promise<PageResponse<ApiKey>> {
  const queryParams = new URLSearchParams()

  if (filter.page !== undefined) queryParams.set('page', String(filter.page))
  if (filter.size !== undefined) queryParams.set('size', String(filter.size))
  if (filter.tenantId) queryParams.set('tenantId', filter.tenantId)
  if (filter.status) queryParams.set('status', filter.status)
  if (filter.prefix) queryParams.set('prefix', filter.prefix)
  if (filter.createdFrom) queryParams.set('createdFrom', filter.createdFrom)
  if (filter.createdTo) queryParams.set('createdTo', filter.createdTo)

  const query = queryParams.toString()

  return apiFetch({
    path: `/admin/api-keys${query ? `?${query}` : ''}`,
    method: 'GET',
  })
}

export async function createApiKey(
  tenantId: string,
): Promise<CreateApiKeyResponse> {
  return apiFetch({
    path: `/admin/tenants/${tenantId}/api-keys`,
    method: 'POST',
  })
}

export async function revokeApiKey(apiKeyId: string) {
  return apiFetch({
    path: `/admin/api-keys/${apiKeyId}/revoke`,
    method: 'POST',
  })
}
