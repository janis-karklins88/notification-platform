import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useParams } from 'react-router-dom'

import {
  deleteEmailTemplate,
  listEmailTemplates,
} from '../api/emailTemplatesApi'
import { EmailTemplatePreviewModal } from '../features/emailTemplates/EmailTemplatePreviewModal'
import { getTenantById, listTenants } from '../api/tenantsApi'
import { EmailTemplateFormModal } from '../features/emailTemplates/EmailTemplateFormModal'
import { EmailTemplatesTable } from '../features/emailTemplates/EmailTemplatesTable'
import type { EmailTemplate } from '../features/emailTemplates/types'
import { notifyDeleted } from '../lib/notifications'

export function EmailTemplatesPage() {
  const { tenantId } = useParams()
  const [isFormModalOpen, setIsFormModalOpen] = useState(false)
  const [selectedTemplate, setSelectedTemplate] = useState<EmailTemplate | null>(null)
  const [previewTemplate, setPreviewTemplate] = useState<EmailTemplate | null>(null)
  const queryClient = useQueryClient()

  const {
    data: templates,
    error,
    isPending,
    refetch,
  } = useQuery({
    queryKey: ['emailTemplates', tenantId],
    queryFn: () => listEmailTemplates(tenantId ?? ''),
  })

  const { data: tenant } = useQuery({
    queryKey: ['tenant', tenantId],
    queryFn: () => getTenantById(tenantId!),
    enabled: Boolean(tenantId),
  })

  const { data: tenantOptions } = useQuery({
    queryKey: ['tenants', 'options'],
    queryFn: () => listTenants({ page: 0, size: 100 }),
  })

  const deleteTemplateMutation = useMutation({
    mutationFn: deleteEmailTemplate,
    onSuccess: async () => {
      notifyDeleted('Email template')
      await queryClient.invalidateQueries({ queryKey: ['emailTemplates', tenantId] })
    },
  })

  function handleCreateOpen() {
    setSelectedTemplate(null)
    setIsFormModalOpen(true)
  }

  function handleEdit(template: EmailTemplate) {
    setSelectedTemplate(template)
    setIsFormModalOpen(true)
  }

  function handleFormClose() {
    setIsFormModalOpen(false)
    setSelectedTemplate(null)
  }

  function handlePreview(template: EmailTemplate) {
    setPreviewTemplate(template)
  }

  function handlePreviewClose() {
    setPreviewTemplate(null)
  }

  function handleRefresh() {
    refetch()
  }

  function handleDelete(templateId: string) {
    if (!window.confirm('Delete this email template?')) {
      return
    }

    deleteTemplateMutation.mutate(templateId)
  }

  const tenantNamesById = Object.fromEntries(
    (tenantOptions?.items ?? []).map((tenantOption) => [tenantOption.id, tenantOption.name]),
  )

  return (
    <section className="space-y-6">
      <header className="flex items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold text-slate-900">
            {tenant ? `${tenant.name} Email Templates` : 'Email Templates'}
          </h1>
          <p className="mt-1 text-sm text-slate-600">
            {tenantId
              ? 'Manage reusable email content for the selected tenant.'
              : 'Review reusable email templates across tenants.'}
          </p>
        </div>
        <div className="flex items-center gap-3">
          <button
            className="rounded-lg border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 transition hover:border-slate-400 hover:bg-slate-100"
            onClick={handleRefresh}
            type="button"
          >
            Refresh
          </button>
          {tenantId ? (
            <button
              className="rounded-lg bg-slate-900 px-4 py-2 text-sm font-medium text-white transition hover:bg-slate-800"
              onClick={handleCreateOpen}
              type="button"
            >
              Create template
            </button>
          ) : null}
        </div>
      </header>

      {error instanceof Error ? (
        <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {error.message}
        </div>
      ) : null}

      <EmailTemplatesTable
        loading={isPending}
        onDelete={handleDelete}
        onEdit={handleEdit}
        onPreview={handlePreview}
        showTenantColumn={!tenantId}
        tenantNamesById={tenantNamesById}
        templates={templates ?? []}
      />

      {tenantId || selectedTemplate ? (
        <EmailTemplateFormModal
          onClose={handleFormClose}
          open={isFormModalOpen}
          template={selectedTemplate}
          tenantId={tenantId ?? selectedTemplate?.tenantId ?? ''}
        />
      ) : null}

      <EmailTemplatePreviewModal
        onClose={handlePreviewClose}
        open={Boolean(previewTemplate)}
        template={previewTemplate}
      />
    </section>
  )
}
