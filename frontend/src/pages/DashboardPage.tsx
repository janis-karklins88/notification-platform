export function DashboardPage() {
  return (
    <div className="min-h-screen bg-slate-950 text-slate-100">
      <main className="mx-auto flex min-h-screen max-w-5xl flex-col justify-center px-6 py-12">
        <p className="text-sm font-medium uppercase tracking-[0.28em] text-slate-400">
          Admin Console
        </p>
        <h1 className="mt-4 text-4xl font-semibold tracking-tight sm:text-5xl">
          Notification Platform
        </h1>
        <p className="mt-6 max-w-2xl text-base leading-7 text-slate-300 sm:text-lg">
          Frontend bootstrap is in place. Routing, API client, and environment
          configuration are ready for the next step.
        </p>
      </main>
    </div>
  )
}
