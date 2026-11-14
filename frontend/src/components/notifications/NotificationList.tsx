import {
  CalendarCheck2,
  CalendarX2,
  CheckCircle2,
  RefreshCw,
  ShieldAlert,
  XCircle,
} from 'lucide-react'
import type { Notification, NotificationType } from '../../types/notification.types'
import { formatRelativeDate } from '../../utils/format'
import { cn } from '../../utils/cn'
import { notificationMeta } from '../../utils/notificationMeta'
import { Button } from '../ui/Button'
import { EmptyState } from '../ui/EmptyState'
import { LoadingState } from '../ui/Spinner'

const ICONS: Record<NotificationType, typeof CheckCircle2> = {
  BACKUP_SUCCESS: CheckCircle2,
  BACKUP_FAILED: XCircle,
  RESTORE_SUCCESS: RefreshCw,
  RESTORE_FAILED: ShieldAlert,
  SCHEDULE_ACTIVATED: CalendarCheck2,
  SCHEDULE_DEACTIVATED: CalendarX2,
}

interface NotificationListProps {
  notifications: Notification[]
  isLoading?: boolean
  onMarkRead?: (id: string) => void
  markingId?: string | null
  compact?: boolean
}

export function NotificationList({
  notifications,
  isLoading,
  onMarkRead,
  markingId,
  compact,
}: NotificationListProps) {
  if (isLoading) {
    return <LoadingState label="Chargement des notifications…" />
  }

  if (notifications.length === 0) {
    return (
      <EmptyState
        icon={CheckCircle2}
        title="Aucune notification"
        description="Vos sauvegardes et restaurations apparaîtront ici."
        className={cn('border-0 bg-transparent py-8', compact && 'py-6')}
      />
    )
  }

  return (
    <ul className="divide-y divide-slate-100">
      {notifications.map((notification) => {
        const meta = notificationMeta(notification.type)
        const Icon = ICONS[notification.type] ?? CheckCircle2
        return (
          <li key={notification.id}>
            <button
              type="button"
              onClick={() => {
                if (!notification.isRead) {
                  onMarkRead?.(notification.id)
                }
              }}
              disabled={notification.isRead || !onMarkRead}
              className={cn(
                'flex w-full items-start gap-3 px-4 py-3 text-left transition',
                notification.isRead ? 'bg-white' : 'bg-brand-50/40 hover:bg-brand-50',
                !notification.isRead && onMarkRead ? 'cursor-pointer' : 'cursor-default',
              )}
            >
              <span
                className={cn(
                  'mt-0.5 flex h-8 w-8 shrink-0 items-center justify-center rounded-xl',
                  meta.className,
                )}
              >
                <Icon className="h-4 w-4" />
              </span>
              <span className="min-w-0 flex-1">
                <span className="flex items-center gap-2">
                  <span className="text-xs font-semibold text-slate-500">{meta.label}</span>
                  {!notification.isRead ? (
                    <span className="h-1.5 w-1.5 rounded-full bg-brand-500" />
                  ) : null}
                </span>
                <span className="mt-0.5 block text-sm text-slate-800">{notification.message}</span>
                <span className="mt-0.5 block text-xs text-slate-400">
                  {markingId === notification.id
                    ? 'Marquage…'
                    : formatRelativeDate(notification.createdAt)}
                </span>
              </span>
            </button>
          </li>
        )
      })}
    </ul>
  )
}

export function MarkAllButton({
  disabled,
  onClick,
}: {
  disabled?: boolean
  onClick: () => void
}) {
  return (
    <Button variant="ghost" size="sm" disabled={disabled} onClick={onClick}>
      Tout marquer comme lu
    </Button>
  )
}
