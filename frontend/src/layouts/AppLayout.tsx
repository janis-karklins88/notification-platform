import { Outlet } from 'react-router-dom'
import { Sidebar } from '../components/layout/AppSidebar'



export function AppLayout() {
  return (
    
    <div className="min-h-screen bg-slate-50 text-slate-950">
      <div className="flex min-h-screen">
        <Sidebar />
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
