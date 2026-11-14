import type { FileDetail } from '../../types/file.types'
import { formatBytes, formatDate } from '../../utils/format'
import { fileTypeLabel } from '../../utils/fileIcon'
import { Skeleton } from '../ui/Skeleton'

export function FileMetadata({ file }: { file: FileDetail }) {
  const rows = [
    { label: 'Type', value: fileTypeLabel(file.mimeType) },
    { label: 'Taille', value: formatBytes(file.fileSize) },
    { label: 'Sauvegardes', value: String(file.backupCount) },
    { label: 'Dernière sauvegarde', value: formatDate(file.lastBackupAt) },
    { label: 'Importé le', value: formatDate(file.createdAt) },
    { label: 'Modifié le', value: formatDate(file.updatedAt) },
  ]

  return (
    <div className="space-y-4">
      <dl className="grid grid-cols-2 gap-3">
        {rows.map((row) => (
          <div key={row.label} className="rounded-xl bg-slate-50 p-3">
            <dt className="text-[10px] font-semibold uppercase tracking-wide text-slate-400">
              {row.label}
            </dt>
            <dd className="mt-0.5 text-sm font-medium text-slate-800">{row.value}</dd>
          </div>
        ))}
      </dl>
      <div>
        <p className="text-[10px] font-semibold uppercase tracking-wide text-slate-400">
          Empreinte SHA-256
        </p>
        <code className="mt-1 block break-all rounded-xl bg-slate-900 px-3 py-2 text-[11px] leading-relaxed text-slate-300">
          {file.sha256Hash}
        </code>
      </div>
      <div>
        <p className="text-[10px] font-semibold uppercase tracking-wide text-slate-400">
          Clé de stockage
        </p>
        <p className="mt-1 break-all text-xs text-slate-500">{file.storedKey}</p>
      </div>
    </div>
  )
}

export function FileMetadataSkeleton() {
  return (
    <div className="space-y-4">
      <div className="grid grid-cols-2 gap-3">
        {Array.from({ length: 6 }).map((_, index) => (
          <div key={index} className="rounded-xl bg-slate-50 p-3">
            <Skeleton className="h-2 w-16" />
            <Skeleton className="mt-2 h-4 w-20" />
          </div>
        ))}
      </div>
      <Skeleton className="h-16 w-full" />
    </div>
  )
}
