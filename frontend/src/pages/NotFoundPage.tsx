import { Link } from 'react-router-dom'

export function NotFoundPage() {
  return (
    <div className="min-h-screen bg-slate-950 text-slate-100">
      <main className="mx-auto flex min-h-screen max-w-5xl flex-col justify-center px-6 py-12">
        <p className="text-sm font-medium uppercase tracking-[0.28em] text-slate-400">
          404
        </p>
        <h1 className="mt-4 text-4xl font-semibold tracking-tight">Page not found</h1>
        <p className="mt-6 max-w-xl text-base leading-7 text-slate-300">
          The route does not exist yet.
        </p>
        <Link
          className="mt-8 inline-flex w-fit rounded-full border border-slate-700 px-4 py-2 text-sm font-medium text-slate-100 transition hover:border-slate-500 hover:bg-slate-900"
          to="/"
        >
          Back to dashboard
        </Link>
      </main>
    </div>
  )
}
