import type { EmailTemplate } from './types'

type EmailTemplatesTableProps = {
  templates: EmailTemplate[]
  loading: boolean
  showTenantColumn?: boolean
  tenantNamesById?: Record<string, string>
  onPreview: (template: EmailTemplate) => void
  onEdit: (template: EmailTemplate) => void
  onDelete: (templateId: string) => void
}

function formatDate(value: string) {
  return new Date(value).toLocaleString()
}

function truncate(value: string, maxLength: number) {
  if (value.length <= maxLength) {
    return value
  }

  return `${value.slice(0, maxLength - 1)}...`
}

export function EmailTemplatesTable({
  templates,
  loading,
  showTenantColumn = false,
  tenantNamesById = {},
  onPreview,
  onEdit,
  onDelete,
}: EmailTemplatesTableProps) {
  if (loading) {
    return (
      <section className="rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
        <p className="text-sm text-slate-600">Loading email templates...</p>
      </section>
    )
  }

  if (templates.length === 0) {
    return (
      <section className="rounded-xl border border-dashed border-slate-300 bg-white p-6 shadow-sm">
        <p className="text-sm font-medium text-slate-900">No email templates found.</p>
        <p className="mt-2 text-sm text-slate-600">
          Create the first tenant template to reuse it in email endpoints.
        </p>
      </section>
    )
  }

  return (
    <section className="overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm">
      <div className="overflow-x-auto">
        <table className="min-w-full divide-y divide-slate-200">
          <thead className="bg-slate-50">
            <tr>
              {showTenantColumn ? (
                <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
                  Tenant
                </th>
              ) : null}
              <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
                Name
              </th>
              <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
                Format
              </th>
              <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
                Subject
              </th>
              <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
                Description
              </th>
              <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
                Created
              </th>
              <th className="px-4 py-3 text-right text-xs font-semibold uppercase tracking-wide text-slate-500">
                
              </th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-200 bg-white">
            {templates.map((template) => (
              <tr key={template.id} className="hover:bg-slate-50">
                {showTenantColumn ? (
                  <td className="px-4 py-3 text-sm text-slate-600">
                    {tenantNamesById[template.tenantId] ?? template.tenantId}
                  </td>
                ) : null}
                <td className="px-4 py-3">
                  <p className="text-sm font-medium text-slate-900">{template.name}</p>
                  
                </td>
                <td className="px-4 py-3 text-sm text-slate-600">
                  {template.html ? 'HTML' : 'Text'}
                </td>
                <td className="px-4 py-3 text-sm text-slate-600">
                  {truncate(template.subject, 72)}
                </td>
                <td className="px-4 py-3 text-sm text-slate-600">
                  {template.description
                    ? truncate(template.description, 72)
                    : 'No description'}
                </td>
                <td className="px-4 py-3 text-sm text-slate-600">
                  {formatDate(template.createdAt)}
                </td>
                <td className="px-4 py-3">
                  <div className="flex justify-end gap-2">
                    <button
                      className="rounded-lg border border-slate-300 px-3 py-1.5 text-sm font-medium text-slate-700 transition hover:border-slate-400 hover:bg-slate-100"
                      onClick={() => onPreview(template)}
                      type="button"
                    >
                      Preview
                    </button>
                    <button
                      className="rounded-lg border border-slate-300 px-3 py-1.5 text-sm font-medium text-slate-700 transition hover:border-slate-400 hover:bg-slate-100"
                      onClick={() => onEdit(template)}
                      type="button"
                    >
                      Edit
                    </button>
                    <button
                      className="rounded-lg border border-red-300 px-3 py-1.5 text-sm font-medium text-red-700 transition hover:border-red-400 hover:bg-red-50"
                      onClick={() => onDelete(template.id)}
                      type="button"
                    >
                      Delete
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  )
}
