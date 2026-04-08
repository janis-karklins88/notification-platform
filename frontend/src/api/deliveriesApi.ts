import { apiFetch } from './client'

import type {
  Delivery,
  DeliveryFilter,
  PageResponse,
} from '../features/deliveries/types'

export async function listDeliveries(
  filter: DeliveryFilter,
): Promise<PageResponse<Delivery>> {
  const queryParams = new URLSearchParams()

  if (filter.page !== undefined) queryParams.set('page', String(filter.page))
  if (filter.size !== undefined) queryParams.set('size', String(filter.size))
  if (filter.status) queryParams.set('status', filter.status)
  if (filter.tenantId) queryParams.set('tenantId', filter.tenantId)
  if (filter.eventId) queryParams.set('eventId', filter.eventId)
  if (filter.endpointId) queryParams.set('endpointId', filter.endpointId)
  if (filter.channel) queryParams.set('channel', filter.channel)
  if (filter.from) queryParams.set('from', filter.from)
  if (filter.to) queryParams.set('to', filter.to)

  const query = queryParams.toString()

  return apiFetch({
    path: `/admin/deliveries${query ? `?${query}` : ''}`,
    method: 'GET',
  })
}

export async function getDeliveryById(deliveryId: string): Promise<Delivery> {
  return apiFetch({
    path: `/admin/deliveries/${deliveryId}`,
    method: 'GET',
  })
}
