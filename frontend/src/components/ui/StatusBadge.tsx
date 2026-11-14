import { cn } from '../../utils/cn'
import type { BackupStatus, RestoreStatus } from '../../types/backup.types'
import type { ScheduleExecutionStatus } from '../../types/schedule.types'
import { Badge } from './Badge'

type Status = BackupStatus | RestoreStatus | ScheduleExecutionStatus

const CONFIG: Record<
  Status,
  { label: string; variant: 'neutral' | 'brand' | 'success' | 'warning' | 'danger' }
> = {
  PENDING: { label: 'En attente', variant: 'warning' },
  IN_PROGRESS: { label: 'En cours', variant: 'brand' },
  SUCCESS: { label: 'Réussi', variant: 'success' },
  FAILED: { label: 'Échoué', variant: 'danger' },
}

export function StatusBadge({ status, className }: { status: Status; className?: string }) {
  const config = CONFIG[status] ?? { label: status, variant: 'neutral' as const }
  return (
    <Badge variant={config.variant} dot className={cn(className)}>
      {config.label}
    </Badge>
  )
}
