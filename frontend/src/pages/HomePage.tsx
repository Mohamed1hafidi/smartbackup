import {
  ArrowRight,
  CalendarClock,
  CheckCircle2,
  Cloud,
  FileClock,
  FolderOpen,
  LayoutDashboard,
  LockKeyhole,
  RotateCcw,
  ShieldCheck,
  Sparkles,
  UploadCloud,
} from 'lucide-react'
import { Link } from 'react-router-dom'
import { APP_NAME } from '../api/config'
import { useAuthStore } from '../store/authStore'

const FEATURES = [
  {
    icon: UploadCloud,
    title: 'Upload simplifié',
    text: 'Glissez-déposez vos fichiers et laissez SmartBackup calculer les empreintes d’intégrité.',
  },
  {
    icon: Cloud,
    title: 'Sauvegarde cloud',
    text: 'Chaque version est stockée dans un bucket privé avec un accès par URL temporaire.',
  },
  {
    icon: CalendarClock,
    title: 'Planifications',
    text: 'Programmez des sauvegardes quotidiennes ou hebdomadaires dans votre fuseau horaire.',
  },
  {
    icon: RotateCcw,
    title: 'Restauration',
    text: 'Revenez à n’importe quelle version en quelques secondes, sans risque pour l’historique.',
  },
  {
    icon: FileClock,
    title: 'Historique complet',
    text: 'Retrouvez toutes vos sauvegardes, leur statut et leur durée d’exécution.',
  },
  {
    icon: ShieldCheck,
    title: 'Sécurité',
    text: 'Authentification JWT, contrôle d’accès par utilisateur et vérification SHA-256.',
  },
]

const STEPS = [
  { icon: FolderOpen, title: 'Importez', text: 'Ajoutez vos documents, images ou archives.' },
  { icon: Cloud, title: 'Sauvegardez', text: 'Lancez une sauvegarde ou planifiez-la.' },
  { icon: RotateCcw, title: 'Restaurez', text: 'Récupérez la bonne version à tout moment.' },
]

export function HomePage() {
  const accessToken = useAuthStore((state) => state.accessToken)

  return (
    <div className="min-h-screen bg-slate-50">
      <header className="sticky top-0 z-30 border-b border-slate-200/80 bg-white/80 backdrop-blur-xl">
        <div className="mx-auto flex h-16 max-w-6xl items-center justify-between px-4 sm:px-6">
          <Link to="/" className="flex items-center gap-2.5 transition-transform duration-200 hover:scale-[1.03]">
            <img
              src="/smartbackup-logo-light.png"
              alt={APP_NAME}
              className="h-12 w-auto max-h-12 object-contain filter drop-shadow-sm"
            />
          </Link>
          <nav className="flex items-center gap-2">
            {accessToken ? (
              <Link
                to="/dashboard"
                className="inline-flex h-10 items-center gap-2 rounded-xl bg-brand-600 px-4 text-sm font-semibold text-white transition hover:bg-brand-700"
              >
                <LayoutDashboard className="h-4 w-4" />
                Mon tableau de bord
              </Link>
            ) : (
              <>
                <Link
                  to="/login"
                  className="hidden h-10 items-center rounded-xl px-4 text-sm font-semibold text-slate-600 transition hover:bg-slate-100 hover:text-slate-900 sm:inline-flex"
                >
                  Connexion
                </Link>
                <Link
                  to="/register"
                  className="inline-flex h-10 items-center gap-2 rounded-xl bg-brand-600 px-4 text-sm font-semibold text-white transition hover:bg-brand-700"
                >
                  Commencer
                  <ArrowRight className="h-4 w-4" />
                </Link>
              </>
            )}
          </nav>
        </div>
      </header>

      <main>
        <section className="relative overflow-hidden">
          <div className="absolute inset-0 bg-[radial-gradient(circle_at_top_right,rgba(37,99,235,0.12),transparent_50%),radial-gradient(circle_at_bottom_left,rgba(6,182,212,0.10),transparent_45%)]" />
          <div className="relative mx-auto grid max-w-6xl gap-12 px-4 py-16 sm:px-6 lg:grid-cols-2 lg:items-center lg:py-24">
            <div>
              <span className="inline-flex items-center gap-2 rounded-full border border-brand-200 bg-white px-3 py-1 text-xs font-semibold text-brand-700">
                <Sparkles className="h-3.5 w-3.5" />
                Stockage cloud sécurisé
              </span>
              <h1 className="mt-6 text-4xl font-bold leading-[1.1] tracking-tight text-slate-950 sm:text-5xl">
                Protégez vos fichiers,
                <span className="block bg-gradient-to-r from-brand-600 to-accent-500 bg-clip-text text-transparent">
                  sans y penser.
                </span>
              </h1>
              <p className="mt-5 max-w-lg text-base leading-relaxed text-slate-600">
                SmartBackup combine sauvegardes manuelles, planifications automatiques et
                restauration versionnée dans une interface simple. Vos données restent les vôtres.
              </p>
              <div className="mt-8 flex flex-col gap-3 sm:flex-row">
                <Link
                  to={accessToken ? '/dashboard' : '/register'}
                  className="inline-flex h-12 items-center justify-center gap-2 rounded-xl bg-brand-600 px-6 text-sm font-semibold text-white shadow-sm transition hover:bg-brand-700"
                >
                  {accessToken ? 'Ouvrir le tableau de bord' : 'Créer mon compte gratuit'}
                  <ArrowRight className="h-4 w-4" />
                </Link>
                <Link
                  to="/login"
                  className="inline-flex h-12 items-center justify-center rounded-xl border border-slate-300 bg-white px-6 text-sm font-semibold text-slate-700 transition hover:bg-slate-50"
                >
                  J’ai déjà un compte
                </Link>
              </div>
              <div className="mt-8 flex flex-wrap items-center gap-x-5 gap-y-2 text-xs text-slate-500">
                <span className="inline-flex items-center gap-1.5">
                  <CheckCircle2 className="h-4 w-4 text-emerald-500" />
                  Aucune carte bancaire
                </span>
                <span className="inline-flex items-center gap-1.5">
                  <LockKeyhole className="h-4 w-4 text-brand-500" />
                  Chiffrement en transit
                </span>
                <span className="inline-flex items-center gap-1.5">
                  <ShieldCheck className="h-4 w-4 text-accent-500" />
                  Intégrité vérifiée
                </span>
              </div>
            </div>

            <div className="relative">
              <div className="absolute -inset-4 rounded-[2rem] bg-gradient-to-br from-brand-200/50 to-accent-200/40 blur-2xl" />
              <div className="relative rounded-3xl border border-white/80 bg-white p-5 shadow-2xl">
                <div className="flex items-center justify-between border-b border-slate-100 pb-4">
                  <div>
                    <p className="text-xs font-medium text-slate-400">Tableau de bord</p>
                    <p className="text-sm font-semibold text-slate-900">Vue d’ensemble</p>
                  </div>
                  <span className="rounded-full bg-emerald-50 px-2.5 py-1 text-[10px] font-bold text-emerald-700">
                    À jour
                  </span>
                </div>
                <div className="mt-4 grid grid-cols-2 gap-3">
                  {[
                    { label: 'Fichiers', value: '24' },
                    { label: 'Sauvegardes', value: '87' },
                    { label: 'Taux de succès', value: '95,4 %' },
                    { label: 'Planifications', value: '3' },
                  ].map((stat) => (
                    <div key={stat.label} className="rounded-xl bg-slate-50 p-3">
                      <p className="text-[10px] font-medium uppercase tracking-wide text-slate-400">
                        {stat.label}
                      </p>
                      <p className="mt-1 text-xl font-bold text-slate-900">{stat.value}</p>
                    </div>
                  ))}
                </div>
                <div className="mt-3 rounded-xl border border-slate-100 p-3">
                  <div className="flex items-center justify-between text-xs">
                    <span className="font-medium text-slate-600">Stockage utilisé</span>
                    <span className="text-slate-400">15 Mo / 1 Go</span>
                  </div>
                  <div className="mt-2 h-2 overflow-hidden rounded-full bg-slate-100">
                    <div className="h-full w-[38%] rounded-full bg-gradient-to-r from-brand-500 to-accent-400" />
                  </div>
                </div>
                <div className="mt-3 space-y-2">
                  {['rapport_annuel.pdf', 'photos_vacances.zip'].map((file, index) => (
                    <div
                      key={file}
                      className="flex items-center gap-2 rounded-lg border border-slate-100 px-3 py-2"
                    >
                      <span className="flex h-7 w-7 items-center justify-center rounded-lg bg-brand-50 text-brand-600">
                        <FolderOpen className="h-3.5 w-3.5" />
                      </span>
                      <span className="min-w-0 flex-1 truncate text-xs font-medium text-slate-700">
                        {file}
                      </span>
                      <span
                        className={`h-1.5 w-1.5 rounded-full ${
                          index === 0 ? 'bg-emerald-500' : 'bg-amber-400'
                        }`}
                      />
                    </div>
                  ))}
                </div>
              </div>
            </div>
          </div>
        </section>

        <section className="border-y border-slate-200 bg-white py-16">
          <div className="mx-auto max-w-6xl px-4 sm:px-6">
            <div className="mx-auto max-w-2xl text-center">
              <h2 className="text-2xl font-bold tracking-tight text-slate-900 sm:text-3xl">
                Tout ce qu’il faut pour dormir tranquille
              </h2>
              <p className="mt-3 text-sm text-slate-500">
                Une suite complète de sauvegarde, pensée pour aller à l’essentiel.
              </p>
            </div>
            <div className="mt-10 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
              {FEATURES.map(({ icon: Icon, title, text }) => (
                <div
                  key={title}
                  className="rounded-2xl border border-slate-200 bg-slate-50/60 p-5 transition hover:border-brand-200 hover:bg-white hover:shadow-soft"
                >
                  <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-white text-brand-600 shadow-sm">
                    <Icon className="h-5 w-5" />
                  </span>
                  <h3 className="mt-4 text-sm font-semibold text-slate-900">{title}</h3>
                  <p className="mt-1.5 text-sm leading-relaxed text-slate-500">{text}</p>
                </div>
              ))}
            </div>
          </div>
        </section>

        <section className="mx-auto max-w-6xl px-4 py-16 sm:px-6">
          <div className="grid gap-4 md:grid-cols-3">
            {STEPS.map(({ icon: Icon, title, text }, index) => (
              <div key={title} className="relative rounded-2xl bg-white p-6 shadow-soft">
                <span className="absolute right-5 top-5 text-3xl font-bold text-slate-100">
                  0{index + 1}
                </span>
                <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-brand-50 text-brand-600">
                  <Icon className="h-5 w-5" />
                </span>
                <h3 className="mt-4 text-sm font-semibold text-slate-900">{title}</h3>
                <p className="mt-1 text-sm text-slate-500">{text}</p>
              </div>
            ))}
          </div>

          <div className="mt-12 overflow-hidden rounded-3xl bg-slate-950 px-6 py-12 text-center sm:px-12">
            <h2 className="text-2xl font-bold tracking-tight text-white sm:text-3xl">
              Prêt à sécuriser vos fichiers ?
            </h2>
            <p className="mx-auto mt-3 max-w-xl text-sm text-slate-400">
              Créez votre compte en moins d’une minute et lancez votre première sauvegarde.
            </p>
            <Link
              to={accessToken ? '/dashboard' : '/register'}
              className="mt-7 inline-flex h-11 items-center gap-2 rounded-xl bg-white px-6 text-sm font-semibold text-slate-900 transition hover:bg-slate-100"
            >
              {accessToken ? 'Accéder à mon espace' : 'Commencer maintenant'}
              <ArrowRight className="h-4 w-4" />
            </Link>
          </div>
        </section>
      </main>

      <footer className="border-t border-slate-200 bg-white py-6">
        <div className="mx-auto flex max-w-6xl flex-col items-center justify-between gap-2 px-4 text-xs text-slate-400 sm:flex-row sm:px-6">
          <p>© {new Date().getFullYear()} {APP_NAME}. Tous droits réservés.</p>
          <p>Sauvegarde cloud · Versioning · Restauration</p>
        </div>
      </footer>
    </div>
  )
}
