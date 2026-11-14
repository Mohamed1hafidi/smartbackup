import { Download, Eye, Loader2, Trash2 } from 'lucide-react'
import type { FileSummary } from '../../types/file.types'
import { fileIcon, fileTypeLabel } from '../../utils/fileIcon'
import { formatBytes, formatDate, formatRelativeDate } from '../../utils/format'
import { Button } from '../ui/Button'
import { Skeleton } from '../ui/Skeleton'
import { FileCard } from './FileCard'
import { ManualBackupButton } from './ManualBackupButton'

interface FileListProps {
  files: FileSummary[]
  isLoading?: boolean
  downloadingId?: string | null
  onView: (file: FileSummary) => void
  onDownload: (file: FileSummary) => void
  onDelete: (file: FileSummary) => void
}

export function FileList({
  files,
  isLoading,
  downloadingId,
  onView,
  onDownload,
  onDelete,
}: FileListProps) {
  if (isLoading) {
    return (
      <div className="space-y-3 p-4">
        {Array.from({ length: 5 }).map((_, index) => (
          <div key={index} className="flex items-center gap-3">
            <Skeleton className="h-10 w-10 rounded-xl" />
            <div className="flex-1 space-y-2">
              <Skeleton className="h-3 w-1/3" />
              <Skeleton className="h-3 w-1/4" />
            </div>
          </div>
        ))}
      </div>
    )
  }

  return (
    <>
      <div className="space-y-3 p-4 md:hidden">
        {files.map((file) => (
          <FileCard
            key={file.id}
            file={file}
            downloading={downloadingId === file.id}
            onView={onView}
            onDownload={onDownload}
            onDelete={onDelete}
          />
        ))}
      </div>

      <div className="hidden overflow-x-auto md:block">
        <table className="w-full text-left text-sm">
          <thead className="border-y border-slate-100 bg-slate-50/70 text-xs uppercase tracking-wide text-slate-400">
            <tr>
              <th className="px-5 py-3 font-semibold">Fichier</th>
              <th className="px-3 py-3 font-semibold">Type</th>
              <th className="px-3 py-3 font-semibold">Taille</th>
              <th className="px-3 py-3 font-semibold">Dernière sauvegarde</th>
              <th className="px-3 py-3 font-semibold">Importé</th>
              <th className="px-5 py-3 text-right font-semibold">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {files.map((file) => {
              const Icon = fileIcon(file.mimeType)
              return (
                <tr key={file.id} className="transition hover:bg-slate-50/70">
                  <td className="px-5 py-3">
                    <div className="flex items-center gap-3">
                      <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-brand-50 text-brand-600">
                        <Icon className="h-4 w-4" />
                      </span>
                      <span className="max-w-[18rem] truncate font-medium text-slate-800">
                        {file.originalName}
                      </span>
                    </div>
                  </td>
                  <td className="px-3 py-3 text-slate-500">{fileTypeLabel(file.mimeType)}</td>
                  <td className="px-3 py-3 text-slate-500">{formatBytes(file.fileSize)}</td>
                  <td className="px-3 py-3 text-slate-500">
                    {file.lastBackupAt ? formatRelativeDate(file.lastBackupAt) : 'Jamais'}
                  </td>
                  <td className="px-3 py-3 text-slate-500">
                    {formatDate(file.createdAt, 'dd MMM yyyy')}
                  </td>
                  <td className="px-5 py-3">
                    <div className="flex items-center justify-end gap-1">
                      <ManualBackupButton
                        fileId={file.id}
                        fileName={file.originalName}
                        size="icon-sm"
                        variant="subtle"
                        showLabel={false}
                      />
                      <Button
                        variant="ghost"
                        size="icon-sm"
                        onClick={() => onView(file)}
                        aria-label={`Voir ${file.originalName}`}
                      >
                        <Eye className="h-4 w-4" />
                      </Button>
                      <Button
                        variant="ghost"
                        size="icon-sm"
                        onClick={() => onDownload(file)}
                        disabled={downloadingId === file.id}
                        aria-label={`Télécharger ${file.originalName}`}
                      >
                        {downloadingId === file.id ? (
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
                        aria-label={`Supprimer ${file.originalName}`}
                      >
                        <Trash2 className="h-4 w-4" />
                      </Button>
                    </div>
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>
    </>
  )
}
