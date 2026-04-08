import { useQuery } from '@tanstack/react-query'

import { getDeliveryById } from '../../api/deliveriesApi'

type DeliveryDetailsModalProps = {
  open: boolean
  deliveryId: string | null
  tenantName?: string
  onClose: () => void
}

function formatDate(value: string | null) {
  return value ? new Date(value).toLocaleString() : 'Never'
}

export function DeliveryDetailsModal({
  open,
  deliveryId,
  tenantName,
  onClose,
}: DeliveryDetailsModalProps) {
  const { data, isPending, error } = useQuery({
    queryKey: ['delivery', deliveryId],
    queryFn: () => getDeliveryById(deliveryId as string),
    enabled: open && Boolean(deliveryId),
  })

  if (!open || !deliveryId) {
    return null
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/50 px-4 py-6">
      <div className="w-full max-w-3xl rounded-2xl bg-white shadow-2xl">
        <div className="border-b border-slate-200 px-6 py-4">
          <h2 className="text-lg font-semibold text-slate-900">Delivery details</h2>
          <p className="mt-1 text-sm text-slate-600">
            Review the delivery metadata and the latest error state.
          </p>
        </div>

        <div className="space-y-6 px-6 py-5">
          {isPending ? (
            <p className="text-sm text-slate-600">Loading delivery details...</p>
          ) : null}

          {error instanceof Error ? (
            <div className="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
              {error.message}
            </div>
          ) : null}

          {data ? (
            <>
              <div className="grid gap-4 md:grid-cols-2">
                <div className="space-y-1">
                  <p className="text-sm font-medium text-slate-700">Tenant</p>
                  <p className="text-sm text-slate-900">{tenantName ?? data.tenantId}</p>
                </div>
                <div className="space-y-1">
                  <p className="text-sm font-medium text-slate-700">Tenant ID</p>
                  <p className="break-all font-mono text-sm text-slate-900">{data.tenantId}</p>
                </div>
                <div className="space-y-1">
                  <p className="text-sm font-medium text-slate-700">Delivery ID</p>
                  <p className="break-all font-mono text-sm text-slate-900">{data.id}</p>
                </div>
                <div className="space-y-1">
                  <p className="text-sm font-medium text-slate-700">Event ID</p>
                  <p className="break-all font-mono text-sm text-slate-900">{data.eventId}</p>
                </div>
                <div className="space-y-1">
                  <p className="text-sm font-medium text-slate-700">Endpoint ID</p>
                  <p className="break-all font-mono text-sm text-slate-900">
                    {data.endpointId}
                  </p>
                </div>
                <div className="space-y-1">
                  <p className="text-sm font-medium text-slate-700">Channel</p>
                  <p className="text-sm text-slate-900">{data.channel}</p>
                </div>
                <div className="space-y-1">
                  <p className="text-sm font-medium text-slate-700">Status</p>
                  <p className="text-sm text-slate-900">{data.status}</p>
                </div>
                <div className="space-y-1">
                  <p className="text-sm font-medium text-slate-700">Last attempt</p>
                  <p className="text-sm text-slate-900">
                    {formatDate(data.lastAttemptAt)}
                  </p>
                </div>
                <div className="space-y-1">
                  <p className="text-sm font-medium text-slate-700">Created</p>
                  <p className="text-sm text-slate-900">{formatDate(data.createdAt)}</p>
                </div>
                <div className="space-y-1">
                  <p className="text-sm font-medium text-slate-700">Updated</p>
                  <p className="text-sm text-slate-900">{formatDate(data.updatedAt)}</p>
                </div>
              </div>

              <div className="space-y-2">
                <p className="text-sm font-medium text-slate-700">Last error</p>
                <pre className="max-h-64 overflow-auto rounded-xl border border-slate-200 bg-slate-50 p-4 text-sm text-slate-900">
                  <code>{data.lastError ?? 'No error recorded.'}</code>
                </pre>
              </div>
            </>
          ) : null}

          <div className="flex justify-end">
            <button
              className="rounded-lg border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 transition hover:border-slate-400 hover:bg-slate-100"
              onClick={onClose}
              type="button"
            >
              Close
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}
