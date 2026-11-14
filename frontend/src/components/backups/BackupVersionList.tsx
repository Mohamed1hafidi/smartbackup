import type { BackupSummary } from '../../types/backup.types'
import { formatBytes, formatDate } from '../../utils/format'
import { cn } from '../../utils/cn'
import { StatusBadge } from '../ui/StatusBadge'

interface BackupVersionListProps {
  backups: BackupSummary[]
  selectedId: string | null
  onSelect: (backup: BackupSummary) => void
}

export function BackupVersionList({ backups, selectedId, onSelect }: BackupVersionListProps) {
  return (
    <ul className="space-y-2">
      {backups.map((backup) => (
        <li key={backup.id}>
          <button
            type="button"
            onClick={() => onSelect(backup)}
            className={cn(
              'flex w-full items-center gap-3 rounded-xl border p-3 text-left transition',
              selectedId === backup.id
                ? 'border-brand-400 bg-brand-50/60 ring-1 ring-brand-300'
                : 'border-slate-200 bg-white hover:border-brand-200 hover:bg-slate-50',
            )}
          >
            <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-slate-100 text-xs font-bold text-slate-600">
              v{backup.versionNumber}
            </span>
            <span className="min-w-0 flex-1">
              <span className="block truncate text-sm font-medium text-slate-800">
                {formatDate(backup.createdAt, 'dd MMM yyyy, HH:mm')}
              </span>
              <span className="block text-xs text-slate-500">
                {formatBytes(backup.fileSize)} · {backup.backupType === 'MANUAL' ? 'Manuelle' : 'Planifiée'}
              </span>
            </span>
            <StatusBadge status={backup.status} />
          </button>
        </li>
      ))}
    </ul>
  )
}
