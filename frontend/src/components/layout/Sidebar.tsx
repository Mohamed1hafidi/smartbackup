import {
  CalendarClock,
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
          'fixed inset-y-0 left-0 z-40 flex w-64 flex-col border-r border-slate-800/80 bg-[#0D1A33] text-white transition-transform duration-300 ease-in-out lg:translate-x-0 shadow-xl shadow-black/20',
          open ? 'translate-x-0' : '-translate-x-full',
        )}
      >
        <div className="flex h-20 items-center justify-center border-b border-white/[0.08] px-4 py-2">
          <img
            src="/smartbackup-logo.png"
            alt={APP_NAME}
            className="h-16 w-auto max-w-[220px] object-contain filter drop-shadow-md transition-transform duration-200 hover:scale-[1.03]"
          />
        </div>

        <nav className="flex-1 space-y-1.5 overflow-y-auto p-3">
          {NAV_ITEMS.map(({ to, label, icon: Icon }) => (
            <NavLink
              key={to}
              to={to}
              onClick={onClose}
              className={({ isActive }) =>
                cn(
                  'group flex items-center gap-3 rounded-xl px-3.5 py-2.5 text-sm font-medium transition-all duration-200 ease-out active:scale-[0.98]',
                  isActive
                    ? 'bg-gradient-to-r from-brand-600 to-blue-600 text-white font-semibold shadow-lg shadow-brand-900/40 translate-x-1'
                    : 'text-slate-300 hover:bg-white/[0.07] hover:text-white hover:translate-x-1',
                )
              }
            >
              {({ isActive }) => (
                <>
                  <Icon
                    className={cn(
                      'h-4 w-4 transition-all duration-200 group-hover:scale-110',
                      isActive ? 'text-white scale-105' : 'text-slate-400 group-hover:text-brand-300',
                    )}
                  />
                  <span>{label}</span>
                </>
              )}
            </NavLink>
          ))}
        </nav>

        <div className="border-t border-white/[0.08] p-4">
          <div className="rounded-xl border border-white/[0.08] bg-white/[0.04] p-3 backdrop-blur-sm shadow-inner transition-colors duration-200 hover:bg-white/[0.06]">
            <div className="flex items-center gap-2">
              <span className="relative flex h-2 w-2">
                <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-emerald-400 opacity-75"></span>
                <span className="relative inline-flex h-2 w-2 rounded-full bg-emerald-500"></span>
              </span>
              <p className="text-xs font-semibold text-slate-200">Stockage sécurisé</p>
            </div>
            <p className="mt-1.5 text-[11px] leading-relaxed text-slate-400">
              Vos fichiers sont protégés et versionnés dans le cloud.
            </p>
          </div>
          <p className="mt-3 text-[10px] font-medium tracking-wider text-slate-500">Version {APP_VERSION}</p>
        </div>
      </aside>
    </>
  )
}
