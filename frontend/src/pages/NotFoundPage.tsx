import { Link } from 'react-router-dom'
import { Compass } from 'lucide-react'

export function NotFoundPage() {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center bg-slate-50 px-4 text-center">
      <span className="flex h-14 w-14 items-center justify-center rounded-2xl bg-white text-brand-600 shadow-soft">
        <Compass className="h-7 w-7" />
      </span>
      <p className="mt-6 text-sm font-semibold uppercase tracking-widest text-brand-600">404</p>
      <h1 className="mt-2 text-2xl font-bold text-slate-900">Page introuvable</h1>
      <p className="mt-2 max-w-sm text-sm text-slate-500">
        La page que vous cherchez n’existe pas ou a été déplacée.
      </p>
      <Link
        to="/"
        className="mt-6 inline-flex h-10 items-center rounded-xl bg-brand-600 px-5 text-sm font-semibold text-white transition hover:bg-brand-700"
      >
        Retour à l’accueil
      </Link>
    </div>
  )
}
