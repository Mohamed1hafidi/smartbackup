import * as DropdownMenu from '@radix-ui/react-dropdown-menu'
import { LogOut, Menu, UserRound } from 'lucide-react'
import { Link, useNavigate } from 'react-router-dom'
import { useLogout } from '../../hooks/useAuth'
import { useAuthStore } from '../../store/authStore'
import { getInitials } from '../../utils/format'
import { NotificationBell } from '../notifications/NotificationBell'

interface NavbarProps {
  onOpenSidebar: () => void
}

export function Navbar({ onOpenSidebar }: NavbarProps) {
  const user = useAuthStore((state) => state.user)
  const logout = useLogout()
  const navigate = useNavigate()

  const handleLogout = () => {
    logout.mutate(undefined, {
      onSettled: () => navigate('/login', { replace: true }),
    })
  }

  return (
    <header className="sticky top-0 z-20 border-b border-slate-200 bg-white/80 backdrop-blur-xl">
      <div className="flex h-16 items-center justify-between gap-3 px-4 sm:px-6 lg:px-8">
        <div className="flex min-w-0 items-center gap-3">
          <button
            type="button"
            onClick={onOpenSidebar}
            className="inline-flex h-10 w-10 items-center justify-center rounded-xl border border-slate-200 text-slate-600 lg:hidden"
            aria-label="Ouvrir le menu"
          >
            <Menu className="h-5 w-5" />
          </button>
          <div className="min-w-0">
            <p className="truncate text-sm font-semibold text-slate-900">
              Bonjour, {user?.fullName?.split(' ')[0] ?? 'utilisateur'} 👋
            </p>
            <p className="hidden truncate text-xs text-slate-500 sm:block">
              Voici l’état de vos sauvegardes aujourd’hui.
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <NotificationBell />
          <DropdownMenu.Root>
            <DropdownMenu.Trigger asChild>
              <button
                type="button"
                className="flex items-center gap-2 rounded-xl border border-slate-200 bg-white p-1 pr-2 transition hover:bg-slate-50"
                aria-label="Menu utilisateur"
              >
                <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-brand-600 text-xs font-bold text-white">
                  {getInitials(user?.fullName)}
                </span>
                <span className="hidden max-w-32 truncate text-xs font-semibold text-slate-700 sm:block">
                  {user?.fullName ?? 'Mon compte'}
                </span>
              </button>
            </DropdownMenu.Trigger>
            <DropdownMenu.Portal>
              <DropdownMenu.Content
                align="end"
                sideOffset={8}
                className="z-50 w-56 rounded-2xl border border-slate-200 bg-white p-1.5 shadow-2xl"
              >
                <div className="px-3 py-2">
                  <p className="truncate text-sm font-semibold text-slate-900">{user?.fullName}</p>
                  <p className="truncate text-xs text-slate-500">{user?.email}</p>
                </div>
                <DropdownMenu.Separator className="my-1 h-px bg-slate-100" />
                <DropdownMenu.Item asChild>
                  <Link
                    to="/profile"
                    className="flex items-center gap-2 rounded-lg px-3 py-2 text-sm text-slate-700 transition hover:bg-slate-100 focus:outline-none"
                  >
                    <UserRound className="h-4 w-4" />
                    Mon profil
                  </Link>
                </DropdownMenu.Item>
                <DropdownMenu.Item
                  onSelect={handleLogout}
                  className="flex cursor-pointer items-center gap-2 rounded-lg px-3 py-2 text-sm text-red-600 transition hover:bg-red-50 focus:outline-none"
                >
                  <LogOut className="h-4 w-4" />
                  {logout.isPending ? 'Déconnexion…' : 'Se déconnecter'}
                </DropdownMenu.Item>
              </DropdownMenu.Content>
            </DropdownMenu.Portal>
          </DropdownMenu.Root>
        </div>
      </div>
    </header>
  )
}
