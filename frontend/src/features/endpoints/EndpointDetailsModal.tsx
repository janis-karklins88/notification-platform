import type { Endpoint } from './types'

type EndpointDetailsModalProps = {
  open: boolean
  endpoint: Endpoint | null
  tenantName?: string
  onClose: () => void
}

function formatDate(value: string) {
  return new Date(value).toLocaleString()
}

export function EndpointDetailsModal({
  open,
  endpoint,
  tenantName,
  onClose,
}: EndpointDetailsModalProps) {
  if (!open || !endpoint) {
    return null
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/50 px-4 py-6">
      <div className="w-full max-w-3xl rounded-2xl bg-white shadow-2xl">
        <div className="border-b border-slate-200 px-6 py-4">
          <h2 className="text-lg font-semibold text-slate-900">Endpoint details</h2>
          <p className="mt-1 text-sm text-slate-600">
            Review the endpoint metadata and full configuration payload.
          </p>
        </div>

        <div className="space-y-6 px-6 py-5">
          <div className="grid gap-4 md:grid-cols-2">
            <div className="space-y-1">
              <p className="text-sm font-medium text-slate-700">Tenant</p>
              <p className="text-sm text-slate-900">{tenantName ?? endpoint.tenantId}</p>
            </div>
            <div className="space-y-1">
              <p className="text-sm font-medium text-slate-700">Tenant ID</p>
              <p className="break-all font-mono text-sm text-slate-900">{endpoint.tenantId}</p>
            </div>
            <div className="space-y-1">
              <p className="text-sm font-medium text-slate-700">Endpoint ID</p>
              <p className="break-all font-mono text-sm text-slate-900">{endpoint.id}</p>
            </div>
            <div className="space-y-1">
              <p className="text-sm font-medium text-slate-700">Type</p>
              <p className="text-sm text-slate-900">{endpoint.type}</p>
            </div>
            <div className="space-y-1">
              <p className="text-sm font-medium text-slate-700">Status</p>
              <p className="text-sm text-slate-900">{endpoint.status}</p>
            </div>
            <div className="space-y-1">
              <p className="text-sm font-medium text-slate-700">Created</p>
              <p className="text-sm text-slate-900">{formatDate(endpoint.createdAt)}</p>
            </div>
            <div className="space-y-1">
              <p className="text-sm font-medium text-slate-700">Updated</p>
              <p className="text-sm text-slate-900">{formatDate(endpoint.updatedAt)}</p>
            </div>
          </div>

          <div className="space-y-2">
            <p className="text-sm font-medium text-slate-700">Config JSON</p>
            <pre className="max-h-96 overflow-auto rounded-xl border border-slate-200 bg-slate-50 p-4 text-sm text-slate-900">
              <code>{JSON.stringify(endpoint.config, null, 2)}</code>
            </pre>
          </div>

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
