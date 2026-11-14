import { Cloud, ShieldCheck, Sparkles, TimerReset } from 'lucide-react'
import { Link, Outlet } from 'react-router-dom'
import { APP_NAME } from '../../api/config'

export function AuthLayout() {
  return (
    <div className="flex min-h-screen bg-slate-50">
      <div className="relative hidden w-1/2 overflow-hidden bg-slate-950 lg:flex">
        <div className="absolute inset-0 bg-[radial-gradient(circle_at_top_left,rgba(37,99,235,0.45),transparent_55%),radial-gradient(circle_at_bottom_right,rgba(6,182,212,0.35),transparent_45%)]" />
        <div className="relative z-10 flex w-full flex-col justify-between p-12">
          <Link to="/" className="flex items-center gap-3 text-white">
            <span className="flex h-11 w-11 items-center justify-center rounded-2xl bg-white/10 ring-1 ring-white/20">
              <Cloud className="h-6 w-6" />
            </span>
            <span className="text-lg font-bold tracking-tight">{APP_NAME}</span>
          </Link>

          <div className="max-w-md">
            <span className="inline-flex items-center gap-2 rounded-full border border-white/15 bg-white/5 px-3 py-1 text-xs font-medium text-brand-100">
              <Sparkles className="h-3.5 w-3.5" />
              Simple, sécurisé, automatique
            </span>
            <h1 className="mt-6 text-4xl font-bold leading-tight tracking-tight text-white">
              Vos fichiers importants, toujours à l’abri.
            </h1>
            <p className="mt-4 text-sm leading-relaxed text-slate-300">
              Sauvegardez, versionnez et restaurez vos fichiers en quelques clics. SmartBackup
              s’occupe du reste.
            </p>

            <div className="mt-8 space-y-4">
              <Feature icon={ShieldCheck} title="Sécurité renforcée" text="JWT, chiffrement et intégrité SHA-256." />
              <Feature icon={TimerReset} title="Planifications flexibles" text="Quotidien ou hebdomadaire, à votre rythme." />
            </div>
          </div>

          <p className="text-xs text-slate-500">
            © {new Date().getFullYear()} {APP_NAME}. Tous droits réservés.
          </p>
        </div>
      </div>

      <div className="flex flex-1 items-center justify-center px-4 py-10 sm:px-8">
        <div className="w-full max-w-md">
          <Link
            to="/"
            className="mb-8 flex items-center justify-center gap-2 text-slate-900 lg:hidden"
          >
            <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-brand-600 text-white">
              <Cloud className="h-5 w-5" />
            </span>
            <span className="text-lg font-bold">{APP_NAME}</span>
          </Link>
          <Outlet />
        </div>
      </div>
    </div>
  )
}

function Feature({
  icon: Icon,
  title,
  text,
}: {
  icon: typeof ShieldCheck
  title: string
  text: string
}) {
  return (
    <div className="flex items-start gap-3">
      <span className="flex h-9 w-9 items-center justify-center rounded-xl bg-white/10 text-brand-200 ring-1 ring-white/10">
        <Icon className="h-4 w-4" />
      </span>
      <div>
        <p className="text-sm font-semibold text-white">{title}</p>
        <p className="text-xs text-slate-400">{text}</p>
      </div>
    </div>
  )
}
