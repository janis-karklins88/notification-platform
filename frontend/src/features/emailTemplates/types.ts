export type EmailTemplate = {
  id: string
  tenantId: string
  name: string
  subject: string
  body: string
  html: boolean
  description: string
  active: boolean
  createdAt: string
}

export type CreateEmailTemplateRequest = {
  name: string
  subject: string
  body: string
  html: boolean
  description?: string
}

export type UpdateEmailTemplateRequest = {
  name: string
  subject: string
  body: string
  html: boolean
  description?: string
}
