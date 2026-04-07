import type { Tenant } from './types'

type TenantsTableProps = {
  tenants: Tenant[]
  loading: boolean
  onEdit: (tenant: Tenant) => void
  page: number
  totalPages: number
  totalElements: number
  hasNext: boolean
  hasPrevious: boolean
  onPageChange: (page: number) => void
}

function formatDate(value: string) {
  return new Date(value).toLocaleString()
}

export function TenantsTable({
  tenants,
  loading,
  onEdit,
  page,
  totalPages,
  totalElements,
  hasNext,
  hasPrevious,
  onPageChange,
}: TenantsTableProps) {
  const shouldShowPagination = totalPages > 1

  if (loading) {
    return (
      <section className="rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
        <p className="text-sm text-slate-600">Loading tenants...</p>
      </section>
    )
  }

  if (tenants.length === 0) {
    return (
      <section className="rounded-xl border border-dashed border-slate-300 bg-white p-6 shadow-sm">
        <p className="text-sm text-slate-600">No tenants found.</p>
      </section>
    )
  }

  return (
    <section className="overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm">
      <div className="overflow-x-auto">
        <table className="min-w-full divide-y divide-slate-200">
          <thead className="bg-slate-50">
            <tr>
              <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
                Name
              </th>
              <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
                Slug
              </th>
              <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
                Status
              </th>
              <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
                Created
              </th>
              <th className="px-4 py-3 text-right text-xs font-semibold uppercase tracking-wide text-slate-500">
                
              </th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-200 bg-white">
            {tenants.map((tenant) => (
              <tr key={tenant.id} className="hover:bg-slate-50">
                <td className="px-4 py-3 text-sm font-medium text-slate-900">
                  {tenant.name}
                </td>
                <td className="px-4 py-3 text-sm text-slate-600">{tenant.slug}</td>
                <td className="px-4 py-3 text-sm text-slate-600">{tenant.status}</td>
                <td className="px-4 py-3 text-sm text-slate-600">
                  {formatDate(tenant.createdAt)}
                </td>
                <td className="px-4 py-3 text-right">
                  <button
                    className="rounded-lg border border-slate-300 px-3 py-1.5 text-sm font-medium text-slate-700 transition hover:border-slate-400 hover:bg-slate-100"
                    onClick={() => onEdit(tenant)}
                    type="button"
                  >
                    Edit
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {shouldShowPagination ? (
        <div className="flex items-center justify-between border-t border-slate-200 bg-slate-50 px-4 py-3">
          <p className="text-sm text-slate-600">
            Page {page + 1} of {totalPages}. Total tenants: {totalElements}
          </p>
          <div className="flex items-center gap-2">
            <button
              className="rounded-lg border border-slate-300 px-3 py-1.5 text-sm font-medium text-slate-700 transition hover:border-slate-400 hover:bg-white disabled:cursor-not-allowed disabled:opacity-60"
              disabled={!hasPrevious}
              onClick={() => onPageChange(page - 1)}
              type="button"
            >
              Previous
            </button>
            <button
              className="rounded-lg border border-slate-300 px-3 py-1.5 text-sm font-medium text-slate-700 transition hover:border-slate-400 hover:bg-white disabled:cursor-not-allowed disabled:opacity-60"
              disabled={!hasNext}
              onClick={() => onPageChange(page + 1)}
              type="button"
            >
              Next
            </button>
          </div>
        </div>
      ) : null}
    </section>
  )
}
