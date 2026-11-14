import type { ScheduleExecution } from '../../types/schedule.types'
import { formatDate, formatDuration } from '../../utils/format'
import { Skeleton } from '../ui/Skeleton'
import { StatusBadge } from '../ui/StatusBadge'

interface ScheduleExecutionListProps {
  executions: ScheduleExecution[]
  isLoading?: boolean
}

export function ScheduleExecutionList({ executions, isLoading }: ScheduleExecutionListProps) {
  if (isLoading) {
    return (
      <div className="space-y-2">
        {Array.from({ length: 4 }).map((_, index) => (
          <Skeleton key={index} className="h-14 w-full" />
        ))}
      </div>
    )
  }

  if (executions.length === 0) {
    return (
      <p className="rounded-xl bg-slate-50 p-6 text-center text-sm text-slate-500">
        Aucune exécution enregistrée pour cette planification.
      </p>
    )
  }

  return (
    <div className="overflow-x-auto">
      <table className="w-full min-w-[520px] text-left text-sm">
        <thead className="border-b border-slate-100 text-xs uppercase tracking-wide text-slate-400">
          <tr>
            <th className="py-2 pr-3 font-semibold">Date</th>
            <th className="px-3 py-2 font-semibold">Statut</th>
            <th className="px-3 py-2 font-semibold">Durée</th>
            <th className="py-2 pl-3 font-semibold">Détail</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-slate-100">
          {executions.map((execution) => (
            <tr key={execution.id}>
              <td className="py-3 pr-3 text-slate-600">
                {formatDate(execution.startedAt, 'dd MMM yyyy, HH:mm')}
              </td>
              <td className="px-3 py-3">
                <StatusBadge status={execution.status} />
              </td>
              <td className="px-3 py-3 text-slate-500">
                {formatDuration(execution.durationMs)}
              </td>
              <td className="py-3 pl-3 text-xs text-red-600">
                {execution.errorMessage ?? <span className="text-slate-400">—</span>}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
