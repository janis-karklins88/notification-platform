import { apiFetch } from './client'

import type {
  CreateSubscriptionRequest,
  PageResponse,
  Subscription,
  SubscriptionFilter,
} from '../features/subscriptions/types'

export async function listSubscriptions(
  filter: SubscriptionFilter,
): Promise<PageResponse<Subscription>> {
  if (!filter.tenantId) {
    throw new Error('Tenant is required to list subscriptions.')
  }

  const queryParams = new URLSearchParams()

  if (filter.page !== undefined) queryParams.set('page', String(filter.page))
  if (filter.size !== undefined) queryParams.set('size', String(filter.size))
  if (filter.eventType) queryParams.set('eventType', filter.eventType)
  if (filter.endpointId) queryParams.set('endpointId', filter.endpointId)
  if (filter.status) queryParams.set('status', filter.status)
  if (filter.createdAfter) queryParams.set('createdAfter', filter.createdAfter)
  if (filter.createdBefore) queryParams.set('createdBefore', filter.createdBefore)

  const query = queryParams.toString()

  return apiFetch({
    path: `/admin/tenants/${filter.tenantId}/subscriptions${query ? `?${query}` : ''}`,
    method: 'GET',
  })
}

export async function createSubscription(
  tenantId: string,
  request: CreateSubscriptionRequest,
): Promise<Subscription> {
  return apiFetch({
    path: `/admin/tenants/${tenantId}/subscriptions`,
    method: 'POST',
    body: JSON.stringify(request),
  })
}

export async function deactivateSubscription(subscriptionId: string) {
  return apiFetch({
    path: `/admin/subscriptions/${subscriptionId}/deactivate`,
    method: 'POST',
  })
}

export async function reactivateSubscription(subscriptionId: string) {
  return apiFetch({
    path: `/admin/subscriptions/${subscriptionId}/reactivate`,
    method: 'POST',
  })
}

export async function deleteSubscription(subscriptionId: string) {
  return apiFetch({
    path: `/admin/subscriptions/${subscriptionId}/delete`,
    method: 'POST',
  })
}
