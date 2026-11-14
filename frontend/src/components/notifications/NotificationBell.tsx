import { useState } from 'react'
import * as DropdownMenu from '@radix-ui/react-dropdown-menu'
import { Bell, CheckCheck, Loader2 } from 'lucide-react'
import { Link } from 'react-router-dom'
import {
  useMarkAllNotificationsRead,
  useMarkNotificationRead,
  useNotifications,
} from '../../hooks/useNotifications'
import { useNotificationStore } from '../../store/notificationStore'
import { useToast } from '../ui/Toast'
import { getApiErrorMessage } from '../../utils/apiError'
import { NotificationList } from './NotificationList'

export function NotificationBell() {
  const [open, setOpen] = useState(false)
  const toast = useToast()
  const unreadCount = useNotificationStore((state) => state.unreadCount)
  const { data, isLoading } = useNotifications({ page: 0, size: 8 })
  const markRead = useMarkNotificationRead()
  const markAll = useMarkAllNotificationsRead()

  const handleMarkRead = (id: string) => {
    markRead.mutate(id, {
      onError: (error) => toast.error(getApiErrorMessage(error)),
    })
  }

  const handleMarkAll = () => {
    markAll.mutate(undefined, {
      onSuccess: () => toast.success('Toutes les notifications ont été marquées comme lues.'),
      onError: (error) => toast.error(getApiErrorMessage(error)),
    })
  }

  return (
    <DropdownMenu.Root open={open} onOpenChange={setOpen}>
      <DropdownMenu.Trigger asChild>
        <button
          type="button"
          className="relative inline-flex h-10 w-10 items-center justify-center rounded-xl border border-slate-200 bg-white text-slate-600 transition hover:bg-slate-50 hover:text-slate-900"
          aria-label="Notifications"
        >
          <Bell className="h-5 w-5" />
          {unreadCount > 0 ? (
            <span className="absolute -right-1 -top-1 flex h-5 min-w-5 items-center justify-center rounded-full bg-brand-600 px-1 text-[10px] font-bold text-white ring-2 ring-white">
              {unreadCount > 9 ? '9+' : unreadCount}
            </span>
          ) : null}
        </button>
      </DropdownMenu.Trigger>
      <DropdownMenu.Portal>
        <DropdownMenu.Content
          align="end"
          sideOffset={8}
          className="z-50 w-[min(22rem,calc(100vw-2rem))] overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-2xl"
        >
          <div className="flex items-center justify-between border-b border-slate-100 px-4 py-3">
            <div>
              <p className="text-sm font-semibold text-slate-900">Notifications</p>
              <p className="text-xs text-slate-500">
                {unreadCount === 0
                  ? 'Tout est à jour'
                  : `${unreadCount} non lue${unreadCount > 1 ? 's' : ''}`}
              </p>
            </div>
            <button
              type="button"
              onClick={handleMarkAll}
              disabled={unreadCount === 0 || markAll.isPending}
              className="inline-flex items-center gap-1 rounded-lg px-2 py-1 text-xs font-semibold text-brand-700 transition hover:bg-brand-50 disabled:opacity-50"
            >
              {markAll.isPending ? (
                <Loader2 className="h-3.5 w-3.5 animate-spin" />
              ) : (
                <CheckCheck className="h-3.5 w-3.5" />
              )}
              Tout lire
            </button>
          </div>
          <div className="max-h-96 overflow-y-auto">
            <NotificationList
              compact
              notifications={data?.content ?? []}
              isLoading={isLoading}
              onMarkRead={handleMarkRead}
              markingId={markRead.isPending ? markRead.variables : null}
            />
          </div>
          <div className="border-t border-slate-100 p-2">
            <DropdownMenu.Item asChild>
              <Link
                to="/notifications"
                onClick={() => setOpen(false)}
                className="block rounded-xl px-3 py-2 text-center text-xs font-semibold text-brand-700 transition hover:bg-brand-50 focus:outline-none"
              >
                Voir toutes les notifications
              </Link>
            </DropdownMenu.Item>
          </div>
        </DropdownMenu.Content>
      </DropdownMenu.Portal>
    </DropdownMenu.Root>
  )
}
