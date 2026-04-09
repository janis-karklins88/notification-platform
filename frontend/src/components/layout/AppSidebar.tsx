import { useQuery } from '@tanstack/react-query'
import { NavLink, useParams } from 'react-router-dom'

import { getTenantById } from '../../api/tenantsApi'

const globalNavItems = [
  { to: '/', label: 'Dashboard' },
  { to: '/tenants', label: 'Tenants' },
  { to: '/deliveries', label: 'Deliveries' },
] as const

function getNavLinkClassName({ isActive }: { isActive: boolean }) {
  return [
    'rounded-lg px-3 py-2 text-sm font-medium transition-colors',
    isActive
      ? 'bg-slate-900 text-white'
      : 'text-slate-600 hover:bg-slate-100 hover:text-slate-900',
  ].join(' ')
}

export const Sidebar = () => {
  const { tenantId } = useParams()
  const { data: tenant } = useQuery({
    queryKey: ['tenant', tenantId],
    queryFn: () => getTenantById(tenantId!),
    enabled: Boolean(tenantId),
  })

  const tenantNavItems = tenantId
    ? [
        { to: `/tenants/${tenantId}`, label: 'Overview', end: true },
        { to: `/tenants/${tenantId}/endpoints`, label: 'Endpoints' },
        { to: `/tenants/${tenantId}/subscriptions`, label: 'Subscriptions' },
        { to: `/tenants/${tenantId}/api-keys`, label: 'API Keys' },
        { to: `/tenants/${tenantId}/email-templates`, label: 'Email Templates' },
      ]
    : []

  return (
    <aside className="w-56 border-r border-slate-200 bg-white px-4 py-6">
      <div className="mb-8">
        <p className="text-xs font-semibold uppercase tracking-[0.24em] text-slate-400">
          Admin panel
        </p>
      </div>

      <div className="space-y-8">
        <div>
          <p className="mb-2 px-3 text-xs font-semibold uppercase tracking-[0.2em] text-slate-400">
            Main
          </p>
          <nav className="flex flex-col gap-1">
            {globalNavItems.map((item) => (
              <NavLink key={item.to} className={getNavLinkClassName} end={item.to === '/'} to={item.to}>
                {item.label}
              </NavLink>
            ))}
          </nav>
        </div>

        {tenantId ? (
          <div>
            <div className="mb-2 px-3">
              <p className="text-xs font-semibold uppercase tracking-[0.2em] text-slate-400">
                Tenant
              </p>
              <p className="mt-2 truncate text-sm font-semibold text-slate-900">
                {tenant?.name ?? 'Tenant'}
              </p>
              
            </div>
            <nav className="flex flex-col gap-1">
              {tenantNavItems.map((item) => (
                <NavLink key={item.to} className={getNavLinkClassName} end={item.end} to={item.to}>
                  {item.label}
                </NavLink>
              ))}
            </nav>
          </div>
        ) : null}
      </div>
    </aside>
  )
}
