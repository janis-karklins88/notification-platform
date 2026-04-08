import type { Delivery } from './types'

type DeliveriesTableProps = {
  deliveries: Delivery[]
  tenantNamesById: Record<string, string>
  loading: boolean
  page: number
  totalPages: number
  totalElements: number
  hasNext: boolean
  hasPrevious: boolean
  onPageChange: (page: number) => void
  onViewDetails: (delivery: Delivery) => void
}

function formatDate(value: string | null) {
  return value ? new Date(value).toLocaleString() : 'Never'
}

export function DeliveriesTable({
  deliveries,
  tenantNamesById,
  loading,
  page,
  totalPages,
  totalElements,
  hasNext,
  hasPrevious,
  onPageChange,
  onViewDetails,
}: DeliveriesTableProps) {
  const shouldShowPagination = totalPages > 1

  if (loading) {
    return (
      <section className="rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
        <p className="text-sm text-slate-600">Loading deliveries...</p>
      </section>
    )
  }

  if (deliveries.length === 0) {
    return (
      <section className="rounded-xl border border-dashed border-slate-300 bg-white p-6 shadow-sm">
        <p className="text-sm text-slate-600">No deliveries found.</p>
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
                Channel
              </th>
              <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
                Status
              </th>
              <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
                Event ID
              </th>
              <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
                Last attempt
              </th>
              <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
                Created
              </th>
              <th className="px-4 py-3 text-right text-xs font-semibold uppercase tracking-wide text-slate-500">
                Actions
              </th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-200 bg-white">
            {deliveries.map((delivery) => (
              <tr key={delivery.id} className="hover:bg-slate-50">
                <td className="px-4 py-3 text-sm text-slate-600">
                  {tenantNamesById[delivery.tenantId] ?? delivery.tenantId}
                </td>
                <td className="px-4 py-3 text-sm font-medium text-slate-900">
                  {delivery.channel}
                </td>
                <td className="px-4 py-3 text-sm text-slate-600">{delivery.status}</td>
                <td className="px-4 py-3 font-mono text-sm text-slate-600">
                  {delivery.eventId}
                </td>
                <td className="px-4 py-3 text-sm text-slate-600">
                  {formatDate(delivery.lastAttemptAt)}
                </td>
                <td className="px-4 py-3 text-sm text-slate-600">
                  {formatDate(delivery.createdAt)}
                </td>
                <td className="px-4 py-3 text-right">
                  <button
                    className="rounded-lg border border-slate-300 px-3 py-1.5 text-sm font-medium text-slate-700 transition hover:border-slate-400 hover:bg-slate-100"
                    onClick={() => onViewDetails(delivery)}
                    type="button"
                  >
                    Details
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
            Page {page + 1} of {totalPages}. Total deliveries: {totalElements}
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
