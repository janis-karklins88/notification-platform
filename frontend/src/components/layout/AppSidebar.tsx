import { NavLink } from 'react-router-dom'

const navItems = [
  { to: '/', label: 'Dashboard' },
  { to: '/tenants', label: 'Tenants' },
  { to: '/endpoints', label: 'Endpoints' },
  { to: '/subscriptions', label: 'Subscriptions' },
  { to: '/deliveries', label: 'Deliveries' },
  { to: '/apikeys', label: 'API Keys' },
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

return (
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
)

}