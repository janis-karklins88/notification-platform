import type { Endpoint } from './types'

type EndpointsTableProps = {
  endpoints: Endpoint[]
  tenantNamesById: Record<string, string>
  loading: boolean
  page: number
  totalPages: number
  totalElements: number
  hasNext: boolean
  hasPrevious: boolean
  onPageChange: (page: number) => void
  onViewDetails: (endpoint: Endpoint) => void
  onEdit: (endpoint: Endpoint) => void
  onDeactivate: (endpointId: string) => void
  onReactivate: (endpointId: string) => void
  onDelete: (endpointId: string) => void
}

function formatDate(value: string) {
  return new Date(value).toLocaleString()
}

export function EndpointsTable({
  endpoints,
  tenantNamesById,
  loading,
  page,
  totalPages,
  totalElements,
  hasNext,
  hasPrevious,
  onPageChange,
  onViewDetails,
  onEdit,
  onDeactivate,
  onReactivate,
  onDelete,
}: EndpointsTableProps) {
  const shouldShowPagination = totalPages > 1

  if (loading) {
    return (
      <section className="rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
        <p className="text-sm text-slate-600">Loading endpoints...</p>
      </section>
    )
  }

  if (endpoints.length === 0) {
    return (
      <section className="rounded-xl border border-dashed border-slate-300 bg-white p-6 shadow-sm">
        <p className="text-sm text-slate-600">No endpoints found.</p>
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
                Tenant
              </th>
              <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
                Type
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
            {endpoints.map((endpoint) => (
              <tr key={endpoint.id} className="hover:bg-slate-50">
                <td className="px-4 py-3 text-sm text-slate-600">
                  {tenantNamesById[endpoint.tenantId] ?? endpoint.tenantId}
                </td>
                <td className="px-4 py-3 text-sm font-medium text-slate-900">{endpoint.type}</td>
                <td className="px-4 py-3 text-sm text-slate-600">{endpoint.status}</td>
                <td className="px-4 py-3 text-sm text-slate-600">
                  {formatDate(endpoint.createdAt)}
                </td>
                <td className="px-4 py-3">
                  {endpoint.status === 'DISABLED' ? (
                    <p className="text-right text-sm text-slate-500">No actions available</p>
                  ) : (
                    <div className="flex justify-end gap-2">
                      <button
                        className="rounded-lg border border-slate-300 px-3 py-1.5 text-sm font-medium text-slate-700 transition hover:border-slate-400 hover:bg-slate-100"
                        onClick={() => onViewDetails(endpoint)}
                        type="button"
                      >
                        Details
                      </button>
                      <button
                        className="rounded-lg border border-slate-300 px-3 py-1.5 text-sm font-medium text-slate-700 transition hover:border-slate-400 hover:bg-slate-100"
                        onClick={() => onEdit(endpoint)}
                        type="button"
                      >
                        Edit
                      </button>
                      {endpoint.status === 'ACTIVE' ? (
                        <button
                          className="rounded-lg border border-slate-300 px-3 py-1.5 text-sm font-medium text-slate-700 transition hover:border-slate-400 hover:bg-slate-100"
                          onClick={() => onDeactivate(endpoint.id)}
                          type="button"
                        >
                          Deactivate
                        </button>
                      ) : (
                        <button
                          className="rounded-lg border border-slate-300 px-3 py-1.5 text-sm font-medium text-slate-700 transition hover:border-slate-400 hover:bg-slate-100"
                          onClick={() => onReactivate(endpoint.id)}
                          type="button"
                        >
                          Reactivate
                        </button>
                      )}
                      <button
                        className="rounded-lg border border-red-300 px-3 py-1.5 text-sm font-medium text-red-700 transition hover:border-red-400 hover:bg-red-50"
                        onClick={() => onDelete(endpoint.id)}
                        type="button"
                      >
                        Delete
                      </button>
                    </div>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {shouldShowPagination ? (
        <div className="flex items-center justify-between border-t border-slate-200 bg-slate-50 px-4 py-3">
          <p className="text-sm text-slate-600">
            Page {page + 1} of {totalPages}. Total endpoints: {totalElements}
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
