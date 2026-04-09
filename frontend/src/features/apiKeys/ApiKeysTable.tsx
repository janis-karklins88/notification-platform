import type { ApiKey } from './types'

type ApiKeysTableProps = {
  apiKeys: ApiKey[]
  loading: boolean
  page: number
  totalPages: number
  totalElements: number
  hasNext: boolean
  hasPrevious: boolean
  onPageChange: (page: number) => void
  onRevoke: (apiKeyId: string) => void
}

function formatDate(value: string | null) {
  return value ? new Date(value).toLocaleString() : 'Never'
}

export function ApiKeysTable({
  apiKeys,
  loading,
  page,
  totalPages,
  totalElements,
  hasNext,
  hasPrevious,
  onPageChange,
  onRevoke,
}: ApiKeysTableProps) {
  const shouldShowPagination = totalPages > 1

  if (loading) {
    return (
      <section className="rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
        <p className="text-sm text-slate-600">Loading API keys...</p>
      </section>
    )
  }

  if (apiKeys.length === 0) {
    return (
      <section className="rounded-xl border border-dashed border-slate-300 bg-white p-6 shadow-sm">
        <p className="text-sm text-slate-600">No API keys found.</p>
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
                Prefix
              </th>
              <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
                Status
              </th>
              <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
                Created
              </th>
              <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
                Last used
              </th>
              <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
                Revoked
              </th>
              <th className="px-4 py-3 text-right text-xs font-semibold uppercase tracking-wide text-slate-500">
                
              </th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-200 bg-white">
            {apiKeys.map((apiKey) => (
              <tr key={apiKey.id} className="hover:bg-slate-50">
                <td className="px-4 py-3 text-sm font-medium text-slate-900">
                  {apiKey.keyPrefix}
                </td>
                <td className="px-4 py-3 text-sm text-slate-600">{apiKey.status}</td>
                <td className="px-4 py-3 text-sm text-slate-600">
                  {formatDate(apiKey.createdAt)}
                </td>
                <td className="px-4 py-3 text-sm text-slate-600">
                  {formatDate(apiKey.lastUsedAt)}
                </td>
                <td className="px-4 py-3 text-sm text-slate-600">
                  {formatDate(apiKey.revokedAt)}
                </td>
                <td className="px-4 py-3 text-right">
                  {apiKey.status !== 'REVOKED' ? (
                    <button
                      className="rounded-lg border border-red-300 px-3 py-1.5 text-sm font-medium text-red-700 transition hover:border-red-400 hover:bg-red-50"
                      onClick={() => onRevoke(apiKey.id)}
                      type="button"
                    >
                      Revoke
                    </button>
                  ) : null}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {shouldShowPagination ? (
        <div className="flex items-center justify-between border-t border-slate-200 bg-slate-50 px-4 py-3">
          <p className="text-sm text-slate-600">
            Page {page + 1} of {totalPages}. Total API keys: {totalElements}
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
