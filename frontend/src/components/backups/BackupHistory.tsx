import { Download, Eye, Loader2, RotateCcw } from 'lucide-react'
import type { BackupSummary } from '../../types/backup.types'
import { formatBytes, formatDate } from '../../utils/format'
import { Badge } from '../ui/Badge'
import { Button } from '../ui/Button'
import { Skeleton } from '../ui/Skeleton'
import { StatusBadge } from '../ui/StatusBadge'

interface BackupHistoryProps {
  backups: BackupSummary[]
  isLoading?: boolean
  downloadingId?: string | null
  onView: (backup: BackupSummary) => void
  onDownload: (backup: BackupSummary) => void
  onRestore: (backup: BackupSummary) => void
}

export function BackupHistory({
  backups,
  isLoading,
  downloadingId,
  onView,
  onDownload,
  onRestore,
}: BackupHistoryProps) {
  if (isLoading) {
    return (
      <div className="space-y-3 p-5">
        {Array.from({ length: 5 }).map((_, index) => (
          <div key={index} className="flex items-center gap-3">
            <Skeleton className="h-9 w-9 rounded-xl" />
            <div className="flex-1 space-y-2">
              <Skeleton className="h-3 w-1/3" />
              <Skeleton className="h-3 w-1/5" />
            </div>
            <Skeleton className="h-6 w-16 rounded-full" />
          </div>
        ))}
      </div>
    )
  }

  return (
    <div className="overflow-x-auto">
      <table className="w-full min-w-[760px] text-left text-sm">
        <thead className="border-b border-slate-100 bg-slate-50/70 text-xs uppercase tracking-wide text-slate-400">
          <tr>
            <th className="px-5 py-3 font-semibold">Fichier</th>
            <th className="px-3 py-3 font-semibold">Version</th>
            <th className="px-3 py-3 font-semibold">Type</th>
            <th className="px-3 py-3 font-semibold">Statut</th>
            <th className="px-3 py-3 font-semibold">Taille</th>
            <th className="px-3 py-3 font-semibold">Date</th>
            <th className="px-5 py-3 text-right font-semibold">Actions</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-slate-100">
          {backups.map((backup) => (
            <tr key={backup.id} className="transition hover:bg-slate-50/70">
              <td className="max-w-[16rem] truncate px-5 py-3 font-medium text-slate-800">
                {backup.fileName}
              </td>
              <td className="px-3 py-3 text-slate-500">v{backup.versionNumber}</td>
              <td className="px-3 py-3">
                <Badge variant={backup.backupType === 'MANUAL' ? 'brand' : 'accent'}>
                  {backup.backupType === 'MANUAL' ? 'Manuelle' : 'Planifiée'}
                </Badge>
              </td>
              <td className="px-3 py-3">
                <StatusBadge status={backup.status} />
              </td>
              <td className="px-3 py-3 text-slate-500">{formatBytes(backup.fileSize)}</td>
              <td className="px-3 py-3 text-slate-500">
                {formatDate(backup.createdAt, 'dd MMM yyyy, HH:mm')}
              </td>
              <td className="px-5 py-3">
                <div className="flex items-center justify-end gap-1">
                  <Button
                    variant="ghost"
                    size="icon-sm"
                    onClick={() => onView(backup)}
                    aria-label="Détails"
                  >
                    <Eye className="h-4 w-4" />
                  </Button>
                  <Button
                    variant="ghost"
                    size="icon-sm"
                    onClick={() => onDownload(backup)}
                    disabled={downloadingId === backup.id}
                    aria-label="Télécharger"
                  >
                    {downloadingId === backup.id ? (
                      <Loader2 className="h-4 w-4 animate-spin" />
                    ) : (
                      <Download className="h-4 w-4" />
                    )}
                  </Button>
                  <Button
                    variant="ghost"
                    size="icon-sm"
                    onClick={() => onRestore(backup)}
                    disabled={backup.status !== 'SUCCESS'}
                    aria-label="Restaurer"
                    title={
                      backup.status === 'SUCCESS'
                        ? 'Restaurer cette version'
                        : 'Seules les sauvegardes réussies peuvent être restaurées'
                    }
                  >
                    <RotateCcw className="h-4 w-4" />
                  </Button>
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
