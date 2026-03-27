import { NavLink, Outlet } from 'react-router-dom'

const navItems = [
  { to: '/', label: 'Dashboard' },
  { to: '/tenants', label: 'Tenants' },
  { to: '/endpoints', label: 'Endpoints' },
  { to: '/subscriptions', label: 'Subscriptions' },
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

export function AppLayout() {
  return (
    <div className="min-h-screen bg-slate-50 text-slate-950">
      <div className="flex min-h-screen">
        <aside className="w-56 border-r border-slate-200 bg-white px-4 py-6">
          <div className="mb-8 ">
            <p className="text-xs font-semibold uppercase tracking-[0.24em] text-slate-400">
              Admin panel
            </p>
          </div>

          <nav className="flex flex-col gap-1">
            {navItems.map((item) => (

              <NavLink key={item.to} className={getNavLinkClassName} to={item.to}>
                {item.label}
              </NavLink>
            ))}
          </nav>
        </aside>

        <div className="flex-1">
          <header className="border-b border-slate-200 bg-white px-6 py-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-xs font-semibold uppercase tracking-[0.24em] text-slate-400">
                  Console
                </p>
                <h1 className="mt-1 text-xl font-semibold text-slate-900">
                  Notification Platform
                </h1>
              </div>
            </div>
          </header>

          <main className="px-6 py-6">
            <Outlet />
          </main>
        </div>
      </div>
    </div>
  )
}
