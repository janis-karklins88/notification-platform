import { useQuery } from '@tanstack/react-query'
import { Link, useParams } from 'react-router-dom'

import { getTenantById } from '../api/tenantsApi'

const tenantSections = [
  {
    title: 'Endpoints',
    description: 'Manage delivery targets configured for this tenant.',
    getTo: (tenantId: string) => `/tenants/${tenantId}/endpoints`,
  },
  {
    title: 'Subscriptions',
    description: 'Control which events route to which endpoints.',
    getTo: (tenantId: string) => `/tenants/${tenantId}/subscriptions`,
  },
  {
    title: 'API Keys',
    description: 'Create and revoke credentials used by this tenant.',
    getTo: (tenantId: string) => `/tenants/${tenantId}/api-keys`,
  },
  {
    title: 'Email Templates',
    description: 'Reserved space for tenant-managed email templates.',
    getTo: (tenantId: string) => `/tenants/${tenantId}/email-templates`,
  },
] as const

export function TenantOverviewPage() {
  const { tenantId } = useParams()
  const { data: tenant, error, isPending } = useQuery({
    queryKey: ['tenant', tenantId],
    queryFn: () => getTenantById(tenantId!),
    enabled: Boolean(tenantId),
  })

  if (isPending) {
    return (
      <section className="rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
        <p className="text-sm text-slate-600">Loading tenant...</p>
      </section>
    )
  }

  if (error instanceof Error) {
    return (
      <section className="rounded-xl border border-red-200 bg-red-50 p-6 shadow-sm">
        <p className="text-sm text-red-700">{error.message}</p>
      </section>
    )
  }

  if (!tenant || !tenantId) {
    return null
  }

  return (
    <section className="space-y-6">
      <header className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
        <p className="text-xs font-semibold uppercase tracking-[0.24em] text-slate-400">
          Tenant Overview
        </p>
        <h1 className="mt-2 text-3xl font-semibold text-slate-900">{tenant.name}</h1>
        <p className="mt-2 text-sm text-slate-600">
          Tenant slug: <span className="font-medium text-slate-900">{tenant.slug}</span>
        </p>
        <p className="mt-1 text-sm text-slate-600">
          Status: <span className="font-medium text-slate-900">{tenant.status}</span>
        </p>
      </header>

      <div className="grid gap-4 md:grid-cols-2">
        {tenantSections.map((section) => (
          <Link
            key={section.title}
            className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm transition hover:-translate-y-0.5 hover:border-slate-300 hover:shadow"
            to={section.getTo(tenantId)}
          >
            <h2 className="text-lg font-semibold text-slate-900">{section.title}</h2>
            <p className="mt-2 text-sm leading-6 text-slate-600">{section.description}</p>
          </Link>
        ))}
      </div>
    </section>
  )
}
