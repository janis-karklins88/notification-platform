export type TenantStatus = 'ACTIVE' | 'SUSPENDED' | 'INACTIVE'

export type Tenant = {
  id: string
  slug: string
  name: string
  status: TenantStatus
  version: number
  createdAt: string
  updatedAt: string
}

export type TenantFilter = {
  page?: number
  size?: number
  status?: TenantStatus
  nameContains?: string
  createdFrom?: string
  createdTo?: string
}

export type CreateTenantRequest = {
  slug: string
  name: string
  status?: TenantStatus
}

export type EditTenantRequest = {
  name?: string
  status?: TenantStatus
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
