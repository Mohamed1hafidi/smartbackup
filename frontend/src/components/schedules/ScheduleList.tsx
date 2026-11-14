import { CalendarClock, History, Pencil, Trash2 } from 'lucide-react'
import type { Schedule } from '../../types/schedule.types'
import { DAYS_OF_WEEK } from '../../utils/constants'
import { formatDate } from '../../utils/format'
import { cn } from '../../utils/cn'
import { Badge } from '../ui/Badge'
import { Button } from '../ui/Button'

interface ScheduleListProps {
  schedules: Schedule[]
  togglingId?: string | null
  onToggle: (schedule: Schedule) => void
  onEdit: (schedule: Schedule) => void
  onDelete: (schedule: Schedule) => void
  onHistory: (schedule: Schedule) => void
}

function scheduleLabel(schedule: Schedule): string {
  const day = schedule.dayOfWeek
    ? DAYS_OF_WEEK.find((item) => item.value === schedule.dayOfWeek)?.label
    : null
  if (schedule.frequency === 'WEEKLY' && day) {
    return `Chaque ${day.toLowerCase()} à ${schedule.scheduledTime}`
  }
  return `Tous les jours à ${schedule.scheduledTime}`
}

export function ScheduleList({
  schedules,
  togglingId,
  onToggle,
  onEdit,
  onDelete,
  onHistory,
}: ScheduleListProps) {
  return (
    <div className="grid gap-4 md:grid-cols-2">
      {schedules.map((schedule) => (
        <div
          key={schedule.id}
          className={cn(
            'rounded-2xl border bg-white p-5 shadow-soft transition',
            schedule.isActive ? 'border-slate-200' : 'border-slate-200 bg-slate-50/60',
          )}
        >
          <div className="flex items-start justify-between gap-3">
            <div className="flex min-w-0 items-start gap-3">
              <span
                className={cn(
                  'flex h-10 w-10 shrink-0 items-center justify-center rounded-xl',
                  schedule.isActive
                    ? 'bg-brand-50 text-brand-600'
                    : 'bg-slate-200 text-slate-500',
                )}
              >
                <CalendarClock className="h-5 w-5" />
              </span>
              <div className="min-w-0">
                <p className="truncate text-sm font-semibold text-slate-900">{schedule.fileName}</p>
                <p className="mt-0.5 text-xs text-slate-500">{scheduleLabel(schedule)}</p>
                <p className="text-[11px] text-slate-400">{schedule.timezone}</p>
              </div>
            </div>
            <button
              type="button"
              role="switch"
              aria-checked={schedule.isActive}
              aria-label={schedule.isActive ? 'Désactiver' : 'Activer'}
              onClick={() => onToggle(schedule)}
              disabled={togglingId === schedule.id}
              className={cn(
                'relative h-6 w-11 shrink-0 rounded-full transition',
                schedule.isActive ? 'bg-brand-600' : 'bg-slate-300',
                togglingId === schedule.id && 'opacity-60',
              )}
            >
              <span
                className={cn(
                  'absolute top-0.5 h-5 w-5 rounded-full bg-white shadow transition',
                  schedule.isActive ? 'left-[1.375rem]' : 'left-0.5',
                )}
              />
            </button>
          </div>

          <div className="mt-4 grid grid-cols-2 gap-2 text-xs">
            <div className="rounded-xl bg-slate-50 p-2.5">
              <p className="text-[10px] font-semibold uppercase tracking-wide text-slate-400">
                Dernière exécution
              </p>
              <p className="mt-0.5 text-slate-700">
                {schedule.lastRunAt ? formatDate(schedule.lastRunAt, 'dd MMM, HH:mm') : 'Jamais'}
              </p>
            </div>
            <div className="rounded-xl bg-slate-50 p-2.5">
              <p className="text-[10px] font-semibold uppercase tracking-wide text-slate-400">
                Prochaine exécution
              </p>
              <p className="mt-0.5 text-slate-700">
                {schedule.isActive && schedule.nextRunAt
                  ? formatDate(schedule.nextRunAt, 'dd MMM, HH:mm')
                  : 'Suspendue'}
              </p>
            </div>
          </div>

          <div className="mt-4 flex items-center justify-between gap-2 border-t border-slate-100 pt-3">
            <div className="flex items-center gap-2">
              <Badge variant={schedule.isActive ? 'success' : 'neutral'} dot>
                {schedule.isActive ? 'Active' : 'Inactive'}
              </Badge>
              <Badge variant={schedule.frequency === 'DAILY' ? 'brand' : 'accent'}>
                {schedule.frequency === 'DAILY' ? 'Quotidienne' : 'Hebdomadaire'}
              </Badge>
            </div>
            <div className="flex items-center gap-1">
              <Button
                variant="ghost"
                size="icon-sm"
                onClick={() => onHistory(schedule)}
                aria-label="Journal d’exécution"
                title="Journal d’exécution"
              >
                <History className="h-4 w-4" />
              </Button>
              <Button
                variant="ghost"
                size="icon-sm"
                onClick={() => onEdit(schedule)}
                aria-label="Modifier"
                title="Modifier"
              >
                <Pencil className="h-4 w-4" />
              </Button>
              <Button
                variant="ghost"
                size="icon-sm"
                className="text-red-500 hover:bg-red-50 hover:text-red-700"
                onClick={() => onDelete(schedule)}
                aria-label="Supprimer"
                title="Supprimer"
              >
                <Trash2 className="h-4 w-4" />
              </Button>
            </div>
          </div>
        </div>
      ))}
    </div>
  )
}
