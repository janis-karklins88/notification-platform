import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useParams } from 'react-router-dom'

import {
  deleteEmailTemplate,
  listEmailTemplates,
} from '../api/emailTemplatesApi'
import { getTenantById } from '../api/tenantsApi'
import { EmailTemplateFormModal } from '../features/emailTemplates/EmailTemplateFormModal'
import { EmailTemplatesTable } from '../features/emailTemplates/EmailTemplatesTable'
import type { EmailTemplate } from '../features/emailTemplates/types'
import { notifyDeleted } from '../lib/notifications'

export function EmailTemplatesPage() {
  const { tenantId } = useParams()
  const [isFormModalOpen, setIsFormModalOpen] = useState(false)
  const [selectedTemplate, setSelectedTemplate] = useState<EmailTemplate | null>(null)
  const queryClient = useQueryClient()

  const {
    data: templates,
    error,
    isPending,
    refetch,
  } = useQuery({
    queryKey: ['emailTemplates', tenantId],
    queryFn: () => listEmailTemplates(tenantId!),
    enabled: Boolean(tenantId),
  })

  const { data: tenant } = useQuery({
    queryKey: ['tenant', tenantId],
    queryFn: () => getTenantById(tenantId!),
    enabled: Boolean(tenantId),
  })

  const deleteTemplateMutation = useMutation({
    mutationFn: deleteEmailTemplate,
    onSuccess: async () => {
      notifyDeleted('Email template')
      await queryClient.invalidateQueries({ queryKey: ['emailTemplates', tenantId] })
    },
  })

  if (!tenantId) {
    return null
  }

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

  function handleRefresh() {
    refetch()
  }

  function handleDelete(templateId: string) {
    if (!window.confirm('Delete this email template?')) {
      return
    }

    deleteTemplateMutation.mutate(templateId)
  }

  return (
    <section className="space-y-6">
      <header className="flex items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold text-slate-900">
            {tenant ? `${tenant.name} Email Templates` : 'Email Templates'}
          </h1>
          <p className="mt-1 text-sm text-slate-600">
            Manage reusable email content for the selected tenant.
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
          <button
            className="rounded-lg bg-slate-900 px-4 py-2 text-sm font-medium text-white transition hover:bg-slate-800"
            onClick={handleCreateOpen}
            type="button"
          >
            Create template
          </button>
        </div>
      </header>

      <div className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
        <p className="text-sm font-medium text-slate-900">Template library</p>
        <p className="mt-2 text-sm text-slate-600">
          Templates created here become selectable in tenant email endpoints. Deleting
          a template soft-disables it, so it disappears from the UI without removing
          historical records.
        </p>
      </div>

      {error instanceof Error ? (
        <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {error.message}
        </div>
      ) : null}

      <EmailTemplatesTable
        loading={isPending}
        onDelete={handleDelete}
        onEdit={handleEdit}
        templates={templates ?? []}
      />

      <EmailTemplateFormModal
        onClose={handleFormClose}
        open={isFormModalOpen}
        template={selectedTemplate}
        tenantId={tenantId}
      />
    </section>
  )
}
