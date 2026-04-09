import { apiFetch } from './client'

import type {
  CreateEmailTemplateRequest,
  EmailTemplate,
  UpdateEmailTemplateRequest,
} from '../features/emailTemplates/types'

export async function listEmailTemplates(tenantId: string): Promise<EmailTemplate[]> {
  const path = tenantId
    ? `/admin/tenants/${tenantId}/email-templates`
    : '/admin/email-templates'

  return apiFetch({
    path,
    method: 'GET',
  })
}

export async function createEmailTemplate(
  tenantId: string,
  request: CreateEmailTemplateRequest,
): Promise<EmailTemplate> {
  return apiFetch({
    path: `/admin/tenants/${tenantId}/email-templates`,
    method: 'POST',
    body: JSON.stringify(request),
  })
}

export async function updateEmailTemplate(
  templateId: string,
  request: UpdateEmailTemplateRequest,
): Promise<EmailTemplate> {
  return apiFetch({
    path: `/admin/email-templates/${templateId}`,
    method: 'PATCH',
    body: JSON.stringify(request),
  })
}

export async function deleteEmailTemplate(templateId: string) {
  return apiFetch({
    path: `/admin/email-templates/${templateId}/delete`,
    method: 'POST',
  })
}
