export type EndpointType = 'EMAIL' | 'SMS' | 'PUSH_NOTIFICATION' | 'WEBHOOK'

export type EndpointStatus = 'ACTIVE' | 'INACTIVE' | 'DISABLED'

export type EndpointConfig = Record<string, unknown>

export type Endpoint = {
  id: string
  tenantId: string
  type: EndpointType
  status: EndpointStatus
  config: EndpointConfig
  createdAt: string
  updatedAt: string
}

export type EndpointFilter = {
  page?: number
  size?: number
  tenantId?: string
  status?: EndpointStatus
  type?: EndpointType
  createdFrom?: string
  createdTo?: string
}

export type CreateEndpointRequest = {
  type: EndpointType
  config: EndpointConfig
}

export type UpdateEndpointRequest = {
  config: EndpointConfig
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
