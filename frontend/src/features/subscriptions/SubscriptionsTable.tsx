import type { Subscription } from './types'

type SubscriptionsTableProps = {
  subscriptions: Subscription[]
  loading: boolean
  emptyMessage: string
  showEmptyState: boolean
  page: number
  totalPages: number
  totalElements: number
  hasNext: boolean
  hasPrevious: boolean
  onPageChange: (page: number) => void
  onDeactivate: (subscriptionId: string) => void
  onReactivate: (subscriptionId: string) => void
  onDelete: (subscriptionId: string) => void
}

function formatDate(value: string) {
  return new Date(value).toLocaleString()
}

export function SubscriptionsTable({
  subscriptions,
  loading,
  emptyMessage,
  showEmptyState,
  page,
  totalPages,
  totalElements,
  hasNext,
  hasPrevious,
  onPageChange,
  onDeactivate,
  onReactivate,
  onDelete,
}: SubscriptionsTableProps) {
  const shouldShowPagination = totalPages > 1

  if (loading) {
    return (
      <section className="rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
        <p className="text-sm text-slate-600">Loading subscriptions...</p>
      </section>
    )
  }

  if (subscriptions.length === 0 && showEmptyState) {
    return (
      <section className="rounded-xl border border-dashed border-slate-300 bg-white p-6 shadow-sm">
        <p className="text-sm text-slate-600">{emptyMessage}</p>
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
                Event type
              </th>
              <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
                Endpoint
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
            {subscriptions.map((subscription) => (
              <tr key={subscription.id} className="hover:bg-slate-50">
                <td className="px-4 py-3 text-sm font-medium text-slate-900">
                  {subscription.eventType}
                </td>
                <td className="px-4 py-3 text-sm text-slate-600">
                  {subscription.endpointID}
                </td>
                <td className="px-4 py-3 text-sm text-slate-600">
                  {subscription.status}
                </td>
                <td className="px-4 py-3 text-sm text-slate-600">
                  {formatDate(subscription.createdAt)}
                </td>
                <td className="px-4 py-3">
                  <div className="flex justify-end gap-2">
                    {subscription.status === 'ACTIVE' ? (
                      <button
                        className="rounded-lg border border-slate-300 px-3 py-1.5 text-sm font-medium text-slate-700 transition hover:border-slate-400 hover:bg-slate-100"
                        onClick={() => onDeactivate(subscription.id)}
                        type="button"
                      >
                        Pause
                      </button>
                    ) : subscription.status === 'PAUSED' ? (
                      <button
                        className="rounded-lg border border-slate-300 px-3 py-1.5 text-sm font-medium text-slate-700 transition hover:border-slate-400 hover:bg-slate-100"
                        onClick={() => onReactivate(subscription.id)}
                        type="button"
                      >
                        Reactivate
                      </button>
                    ) : null}
                    {subscription.status !== 'DELETED' ? (
                      <button
                        className="rounded-lg border border-red-300 px-3 py-1.5 text-sm font-medium text-red-700 transition hover:border-red-400 hover:bg-red-50"
                        onClick={() => onDelete(subscription.id)}
                        type="button"
                      >
                        Delete
                      </button>
                    ) : null}
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {shouldShowPagination ? (
        <div className="flex items-center justify-between border-t border-slate-200 bg-slate-50 px-4 py-3">
          <p className="text-sm text-slate-600">
            Page {page + 1} of {totalPages}. Total subscriptions: {totalElements}
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
