import {
  CalendarClock,
  Cloud,
  FolderOpen,
  History,
  LayoutDashboard,
  RotateCcw,
  UserRound,
} from 'lucide-react'
import { NavLink } from 'react-router-dom'
import { APP_NAME, APP_VERSION } from '../../api/config'
import { cn } from '../../utils/cn'

const NAV_ITEMS = [
  { to: '/dashboard', label: 'Tableau de bord', icon: LayoutDashboard },
  { to: '/files', label: 'Fichiers', icon: FolderOpen },
  { to: '/backups', label: 'Sauvegardes', icon: History },
  { to: '/restore', label: 'Restauration', icon: RotateCcw },
  { to: '/schedules', label: 'Planifications', icon: CalendarClock },
  { to: '/profile', label: 'Mon profil', icon: UserRound },
]

interface SidebarProps {
  open: boolean
  onClose: () => void
}

export function Sidebar({ open, onClose }: SidebarProps) {
  return (
    <>
      <div
        className={cn(
          'fixed inset-0 z-30 bg-slate-950/40 backdrop-blur-sm transition-opacity lg:hidden',
          open ? 'opacity-100' : 'pointer-events-none opacity-0',
        )}
        onClick={onClose}
        aria-hidden
      />
      <aside
        className={cn(
          'fixed inset-y-0 left-0 z-40 flex w-64 flex-col border-r border-slate-200 bg-white transition-transform duration-200 lg:translate-x-0',
          open ? 'translate-x-0' : '-translate-x-full',
        )}
      >
        <div className="flex h-16 items-center gap-2.5 border-b border-slate-100 px-5">
          <span className="flex h-9 w-9 items-center justify-center rounded-xl bg-brand-600 text-white shadow-sm">
            <Cloud className="h-5 w-5" />
          </span>
          <div>
            <p className="text-sm font-bold tracking-tight text-slate-900">{APP_NAME}</p>
            <p className="text-[10px] font-medium uppercase tracking-widest text-slate-400">
              Cloud backup
            </p>
          </div>
        </div>

        <nav className="flex-1 space-y-1 overflow-y-auto p-3">
          {NAV_ITEMS.map(({ to, label, icon: Icon }) => (
            <NavLink
              key={to}
              to={to}
              onClick={onClose}
              className={({ isActive }) =>
                cn(
                  'flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition',
                  isActive
                    ? 'bg-brand-50 text-brand-700'
                    : 'text-slate-600 hover:bg-slate-100 hover:text-slate-900',
                )
              }
            >
              <Icon className="h-4 w-4" />
              {label}
            </NavLink>
          ))}
        </nav>

        <div className="border-t border-slate-100 p-4">
          <div className="rounded-xl bg-slate-50 p-3">
            <p className="text-xs font-semibold text-slate-700">Stockage sécurisé</p>
            <p className="mt-1 text-[11px] leading-relaxed text-slate-500">
              Vos fichiers sont protégés et versionnés dans le cloud.
            </p>
          </div>
          <p className="mt-3 text-[10px] text-slate-400">Version {APP_VERSION}</p>
        </div>
      </aside>
    </>
  )
}
