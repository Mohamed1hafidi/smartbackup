import type { BackupDetail } from '../../types/backup.types'
import { formatBytes, formatDate } from '../../utils/format'
import { Badge } from '../ui/Badge'
import { StatusBadge } from '../ui/StatusBadge'

export function BackupDetails({ backup }: { backup: BackupDetail }) {
  const rows = [
    { label: 'Fichier', value: backup.fileName },
    { label: 'Version', value: `v${backup.versionNumber}` },
    { label: 'Taille', value: formatBytes(backup.fileSize) },
    { label: 'Créée le', value: formatDate(backup.createdAt) },
    { label: 'Terminée le', value: formatDate(backup.completedAt) },
  ]

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center gap-2">
        <StatusBadge status={backup.status} />
        <Badge variant={backup.backupType === 'MANUAL' ? 'brand' : 'accent'}>
          {backup.backupType === 'MANUAL' ? 'Manuelle' : 'Planifiée'}
        </Badge>
      </div>

      <dl className="grid grid-cols-2 gap-3">
        {rows.map((row) => (
          <div key={row.label} className="rounded-xl bg-slate-50 p-3">
            <dt className="text-[10px] font-semibold uppercase tracking-wide text-slate-400">
              {row.label}
            </dt>
            <dd className="mt-0.5 truncate text-sm font-medium text-slate-800">{row.value}</dd>
          </div>
        ))}
      </dl>

      {backup.errorMessage ? (
        <div className="rounded-xl border border-red-200 bg-red-50 p-3 text-sm text-red-700">
          {backup.errorMessage}
        </div>
      ) : null}

      <div>
        <p className="text-[10px] font-semibold uppercase tracking-wide text-slate-400">
          Empreinte SHA-256
        </p>
        <code className="mt-1 block break-all rounded-xl bg-slate-900 px-3 py-2 text-[11px] leading-relaxed text-slate-300">
          {backup.sha256Hash}
        </code>
      </div>

      <div className="grid gap-1 text-xs text-slate-500">
        <p>
          <span className="font-semibold text-slate-600">Bucket :</span> {backup.r2Bucket}
        </p>
        <p className="break-all">
          <span className="font-semibold text-slate-600">Objet :</span> {backup.r2ObjectKey}
        </p>
      </div>
    </div>
  )
}
