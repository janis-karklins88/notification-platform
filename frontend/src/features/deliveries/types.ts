export type DeliveryStatus = 'PENDING' | 'IN_PROGRESS' | 'DELIVERED' | 'FAILED'

export type DeliveryChannel = 'EMAIL' | 'SMS' | 'PUSH_NOTIFICATION' | 'WEBHOOK'

export type Delivery = {
  id: string
  tenantId: string
  eventId: string
  endpointId: string
  channel: DeliveryChannel
  status: DeliveryStatus
  lastAttemptAt: string | null
  lastError: string | null
  createdAt: string
  updatedAt: string
}

export type DeliveryFilter = {
  page?: number
  size?: number
  status?: DeliveryStatus
  tenantId?: string
  eventId?: string
  endpointId?: string
  channel?: DeliveryChannel
  from?: string
  to?: string
}

export type PageResponse<T> = {
  items: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  hasNext: boolean
  hasPrevious: boolean
}
