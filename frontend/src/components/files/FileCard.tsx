import { Download, Eye, Loader2, Trash2 } from 'lucide-react'
import type { FileSummary } from '../../types/file.types'
import { fileIcon, fileTypeLabel } from '../../utils/fileIcon'
import { formatBytes, formatDate, formatRelativeDate } from '../../utils/format'
import { Button } from '../ui/Button'
import { ManualBackupButton } from './ManualBackupButton'

interface FileCardProps {
  file: FileSummary
  downloading?: boolean
  onView: (file: FileSummary) => void
  onDownload: (file: FileSummary) => void
  onDelete: (file: FileSummary) => void
}

export function FileCard({ file, downloading, onView, onDownload, onDelete }: FileCardProps) {
  const Icon = fileIcon(file.mimeType)

  return (
    <div className="rounded-2xl border border-slate-200 bg-white p-4">
      <div className="flex items-start gap-3">
        <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-brand-50 text-brand-600">
          <Icon className="h-5 w-5" />
        </span>
        <div className="min-w-0 flex-1">
          <p className="truncate text-sm font-semibold text-slate-900">{file.originalName}</p>
          <p className="mt-0.5 text-xs text-slate-500">
            {fileTypeLabel(file.mimeType)} · {formatBytes(file.fileSize)}
          </p>
          <p className="mt-1 text-[11px] text-slate-400">
            Dernière sauvegarde : {file.lastBackupAt ? formatRelativeDate(file.lastBackupAt) : 'jamais'}
          </p>
        </div>
      </div>
      <div className="mt-3 flex items-center justify-between gap-2 border-t border-slate-100 pt-3">
        <ManualBackupButton fileId={file.id} fileName={file.originalName} size="sm" />
        <div className="flex items-center gap-1">
          <Button variant="ghost" size="icon-sm" onClick={() => onView(file)} aria-label="Détails">
            <Eye className="h-4 w-4" />
          </Button>
          <Button
            variant="ghost"
            size="icon-sm"
            onClick={() => onDownload(file)}
            disabled={downloading}
            aria-label="Télécharger"
          >
            {downloading ? (
              <Loader2 className="h-4 w-4 animate-spin" />
            ) : (
              <Download className="h-4 w-4" />
            )}
          </Button>
          <Button
            variant="ghost"
            size="icon-sm"
            className="text-red-500 hover:bg-red-50 hover:text-red-700"
            onClick={() => onDelete(file)}
            aria-label="Supprimer"
          >
            <Trash2 className="h-4 w-4" />
          </Button>
        </div>
      </div>
      <p className="mt-2 text-[10px] text-slate-400">Importé le {formatDate(file.createdAt)}</p>
    </div>
  )
}
