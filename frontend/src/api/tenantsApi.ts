import { apiFetch } from "./client";

import type {
  CreateTenantRequest,
  EditTenantRequest,
  PageResponse,
  Tenant,
  TenantFilter,
} from "../features/tenants/types";

export async function listTenants(
  filter:TenantFilter,
): Promise<PageResponse<Tenant>> {
  const queryParams = new URLSearchParams();
  if(filter.page !== undefined) queryParams.set('page', String(filter.page));
  if(filter.size !== undefined) queryParams.set('size', String(filter.size));
  if(filter.status !== undefined) queryParams.set('status', filter.status);
  if(filter.nameContains !== undefined) queryParams.set('nameContains', filter.nameContains);
  if(filter.createdFrom !== undefined) queryParams.set('createdFrom', filter.createdFrom);
  if(filter.createdTo !== undefined) queryParams.set('createdTo', filter.createdTo);

  const query = queryParams.toString();
  return apiFetch({
    path: `/admin/tenants${query ? `?${query}` : ''}`,
    method: 'GET',
  })
}

export async function createTenant(request: CreateTenantRequest): Promise<Tenant> {
  return apiFetch({
    path: '/admin/tenants',
    method: 'POST',
    body: JSON.stringify(request),
  })
}

export async function editTenant(tenantId: string, request: EditTenantRequest): Promise<Tenant> {
  return apiFetch({
    path: `/admin/tenants/${tenantId}`,
    method: 'PATCH',
    body: JSON.stringify(request),
  })
}

export async function getTenantById(tenantId: string): Promise<Tenant> {
  return apiFetch({
    path: `/admin/tenants/${tenantId}`,
    method: 'GET',
  })
}