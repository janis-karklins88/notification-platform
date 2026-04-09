import { apiFetch } from './client'

import type {
  CreateEndpointRequest,
  EmailTemplate,
  Endpoint,
  EndpointFilter,
  PageResponse,
  UpdateEndpointRequest,
} from '../features/endpoints/types'

export async function listEndpoints(
  filter: EndpointFilter,
): Promise<PageResponse<Endpoint>> {
  const queryParams = new URLSearchParams()

  if (filter.page !== undefined) queryParams.set('page', String(filter.page))
  if (filter.size !== undefined) queryParams.set('size', String(filter.size))
  if (filter.tenantId) queryParams.set('tenantId', filter.tenantId)
  if (filter.status) queryParams.set('status', filter.status)
  if (filter.type) queryParams.set('type', filter.type)
  if (filter.createdFrom) queryParams.set('createdFrom', filter.createdFrom)
  if (filter.createdTo) queryParams.set('createdTo', filter.createdTo)

  const query = queryParams.toString()

  return apiFetch({
    path: `/admin/endpoints${query ? `?${query}` : ''}`,
    method: 'GET',
  })
}

export async function createEndpoint(
  tenantId: string,
  request: CreateEndpointRequest,
): Promise<Endpoint> {
  return apiFetch({
    path: `/admin/tenants/${tenantId}/endpoints`,
    method: 'POST',
    body: JSON.stringify(request),
  })
}

export async function updateEndpoint(
  endpointId: string,
  request: UpdateEndpointRequest,
): Promise<Endpoint> {
  return apiFetch({
    path: `/admin/endpoints/${endpointId}`,
    method: 'PATCH',
    body: JSON.stringify(request),
  })
}

export async function listEmailTemplates(tenantId: string): Promise<EmailTemplate[]> {
  return apiFetch({
    path: `/admin/tenants/${tenantId}/email-templates`,
    method: 'GET',
  })
}

export async function deactivateEndpoint(endpointId: string) {
  return apiFetch({
    path: `/admin/endpoints/${endpointId}/deactivate`,
    method: 'POST',
  })
}

export async function reactivateEndpoint(endpointId: string) {
  return apiFetch({
    path: `/admin/endpoints/${endpointId}/reactivate`,
    method: 'POST',
  })
}

export async function deleteEndpoint(endpointId: string) {
  return apiFetch({
    path: `/admin/endpoints/${endpointId}/delete`,
    method: 'POST',
  })
}
