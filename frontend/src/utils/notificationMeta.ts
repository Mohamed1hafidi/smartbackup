import type { NotificationType } from '../types/notification.types'

export function notificationMeta(type: NotificationType): {
  className: string
  label: string
} {
  switch (type) {
    case 'BACKUP_SUCCESS':
      return { className: 'bg-emerald-50 text-emerald-600', label: 'Sauvegarde réussie' }
    case 'BACKUP_FAILED':
      return { className: 'bg-red-50 text-red-600', label: 'Sauvegarde échouée' }
    case 'RESTORE_SUCCESS':
      return { className: 'bg-accent-50 text-accent-600', label: 'Restauration réussie' }
    case 'RESTORE_FAILED':
      return { className: 'bg-red-50 text-red-600', label: 'Restauration échouée' }
    case 'SCHEDULE_ACTIVATED':
      return { className: 'bg-brand-50 text-brand-600', label: 'Planification activée' }
    case 'SCHEDULE_DEACTIVATED':
      return { className: 'bg-amber-50 text-amber-600', label: 'Planification désactivée' }
    default:
      return { className: 'bg-slate-100 text-slate-600', label: 'Notification' }
  }
}
