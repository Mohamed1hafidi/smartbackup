import { useState } from 'react'
import { Bell, BellRing, RefreshCw } from 'lucide-react'
import {
  MarkAllButton,
  NotificationList,
} from '../components/notifications/NotificationList'
import { Button } from '../components/ui/Button'
import { Card } from '../components/ui/Card'
import { Pagination } from '../components/ui/Pagination'
import { useToast } from '../components/ui/Toast'
import {
  useMarkAllNotificationsRead,
  useMarkNotificationRead,
  useNotifications,
} from '../hooks/useNotifications'
import type { NotificationListParams } from '../types/notification.types'
import { getApiErrorMessage } from '../utils/apiError'

type Filter = 'all' | 'unread' | 'read'

const FILTERS: { value: Filter; label: string }[] = [
  { value: 'all', label: 'Toutes' },
  { value: 'unread', label: 'Non lues' },
  { value: 'read', label: 'Lues' },
]

export function NotificationsPage() {
  const [filter, setFilter] = useState<Filter>('all')
  const [page, setPage] = useState(0)
  const toast = useToast()

  const params: NotificationListParams = {
    page,
    size: 20,
    ...(filter === 'unread' ? { isRead: false } : {}),
    ...(filter === 'read' ? { isRead: true } : {}),
  }

  const notifications = useNotifications(params)
  const markRead = useMarkNotificationRead()
  const markAll = useMarkAllNotificationsRead()

  const unreadCount = notifications.data?.unreadCount ?? 0
  const items = notifications.data?.content ?? []

  const handleMarkAll = () => {
    markAll.mutate(undefined, {
      onSuccess: (result) =>
        toast.success(
          result.data.updatedCount > 0
            ? `${result.data.updatedCount} notification${result.data.updatedCount > 1 ? 's' : ''} marquée${
                result.data.updatedCount > 1 ? 's' : ''
              } comme lue${result.data.updatedCount > 1 ? 's' : ''}.`
            : 'Aucune notification à marquer.',
        ),
      onError: (error) => toast.error(getApiErrorMessage(error)),
    })
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col justify-between gap-3 sm:flex-row sm:items-end">
        <div>
          <p className="text-xs font-semibold uppercase tracking-widest text-brand-600">
            Centre d’activité
          </p>
          <h1 className="mt-1 text-2xl font-bold tracking-tight text-slate-900">Notifications</h1>
          <p className="mt-1 text-sm text-slate-500">
            {unreadCount > 0
              ? `${unreadCount} notification${unreadCount > 1 ? 's' : ''} non lue${
                  unreadCount > 1 ? 's' : ''
                }`
              : 'Vous êtes à jour.'}
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Button
            variant="outline"
            onClick={() => void notifications.refetch()}
            disabled={notifications.isFetching}
          >
            <RefreshCw className={`h-4 w-4 ${notifications.isFetching ? 'animate-spin' : ''}`} />
            Actualiser
          </Button>
          <MarkAllButton
            disabled={unreadCount === 0 || markAll.isPending}
            onClick={handleMarkAll}
          />
        </div>
      </div>

      <Card>
        <div className="flex flex-col gap-3 border-b border-slate-100 p-4 sm:flex-row sm:items-center">
          <div className="flex rounded-xl bg-slate-100 p-1">
            {FILTERS.map((item) => (
              <button
                key={item.value}
                type="button"
                onClick={() => {
                  setFilter(item.value)
                  setPage(0)
                }}
                className={`rounded-lg px-3 py-1.5 text-xs font-semibold transition ${
                  filter === item.value
                    ? 'bg-white text-slate-900 shadow-sm'
                    : 'text-slate-500 hover:text-slate-700'
                }`}
              >
                {item.label}
                {item.value === 'unread' && unreadCount > 0 ? (
                  <span className="ml-1.5 rounded-full bg-brand-600 px-1.5 py-0.5 text-[10px] text-white">
                    {unreadCount > 99 ? '99+' : unreadCount}
                  </span>
                ) : null}
              </button>
            ))}
          </div>
          <span className="text-xs text-slate-400 sm:ml-auto">
            {notifications.data ? `${notifications.data.totalElements} au total` : ''}
          </span>
        </div>

        {notifications.isError ? (
          <div className="flex flex-col items-start justify-between gap-3 border-b border-red-100 bg-red-50 p-4 sm:flex-row sm:items-center">
            <p className="text-sm text-red-700">{getApiErrorMessage(notifications.error)}</p>
            <Button variant="outline" size="sm" onClick={() => void notifications.refetch()}>
              Réessayer
            </Button>
          </div>
        ) : null}

        {items.length === 0 && !notifications.isLoading && filter !== 'all' ? (
          <div className="p-8">
            <div className="mx-auto flex max-w-sm flex-col items-center text-center">
              {filter === 'unread' ? (
                <Bell className="h-10 w-10 text-emerald-500" />
              ) : (
                <BellRing className="h-10 w-10 text-slate-300" />
              )}
              <p className="mt-3 text-sm font-semibold text-slate-900">
                {filter === 'unread' ? 'Aucune notification non lue' : 'Aucune notification lue'}
              </p>
              <p className="mt-1 text-xs text-slate-500">
                {filter === 'unread'
                  ? 'Tout est à jour.'
                  : 'Les notifications lues apparaîtront ici.'}
              </p>
            </div>
          </div>
        ) : (
          <NotificationList
            notifications={items}
            isLoading={notifications.isLoading}
            markingId={markRead.isPending ? (markRead.variables ?? null) : null}
            onMarkRead={(id) =>
              markRead.mutate(id, {
                onError: (error) => toast.error(getApiErrorMessage(error)),
              })
            }
          />
        )}

        {notifications.data && notifications.data.totalPages > 1 ? (
          <div className="border-t border-slate-100 px-4 pb-4">
            <Pagination
              page={page}
              totalPages={notifications.data.totalPages}
              totalElements={notifications.data.totalElements}
              onPageChange={setPage}
            />
          </div>
        ) : null}
      </Card>
    </div>
  )
}
