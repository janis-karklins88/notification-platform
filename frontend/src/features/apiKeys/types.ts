export type ApiKeyStatus = 'ACTIVE' | 'INACTIVE' | 'REVOKED'

export type ApiKey = {
  id: string
  keyPrefix: string
  status: ApiKeyStatus
  createdAt: string
  revokedAt: string | null
  lastUsedAt: string | null
}

export type ApiKeyFilter = {
  page?: number
  size?: number
  tenantId?: string
  status?: ApiKeyStatus
  prefix?: string
  createdFrom?: string
  createdTo?: string
}

export type CreateApiKeyResponse = {
  id: string
  tenantId: string
  keyPrefix: string
  plaintextKey: string
  status: ApiKeyStatus
  createdAt: string
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
