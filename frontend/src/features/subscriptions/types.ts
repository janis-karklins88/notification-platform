export type SubscriptionStatus = 'ACTIVE' | 'PAUSED' | 'DELETED'

export type Subscription = {
  id: string
  tenantId: string
  eventType: string
  status: SubscriptionStatus
  endpointID: string
  createdAt: string
}

export type SubscriptionFilter = {
  tenantId?: string
  page?: number
  size?: number
  eventType?: string
  endpointId?: string
  status?: SubscriptionStatus
  createdAfter?: string
  createdBefore?: string
}

export type CreateSubscriptionRequest = {
  eventType: string
  endpointId: string
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
