import { useDeferredValue, useEffect, useState } from 'react'
import {
  ArrowDownWideNarrow,
  ArrowUpWideNarrow,
  FolderOpen,
  Search,
  UploadCloud,
} from 'lucide-react'
import { FileList } from '../components/files/FileList'
import { FileMetadata, FileMetadataSkeleton } from '../components/files/FileMetadata'
import { FileUploader } from '../components/files/FileUploader'
import { ManualBackupButton } from '../components/files/ManualBackupButton'
import { Button } from '../components/ui/Button'
import { Card } from '../components/ui/Card'
import { ConfirmDialog } from '../components/ui/ConfirmDialog'
import { EmptyState } from '../components/ui/EmptyState'
import { Input, Select } from '../components/ui/Input'
import { Modal } from '../components/ui/Modal'
import { Pagination } from '../components/ui/Pagination'
import { useDeleteFile, useDownloadFile, useFile, useFiles } from '../hooks/useFiles'
import { useToast } from '../components/ui/Toast'
import type { FileSort, FileSummary, SortDirection } from '../types/file.types'
import { getApiErrorMessage } from '../utils/apiError'
import { formatBytes } from '../utils/format'

export function FilesPage() {
  const [search, setSearch] = useState('')
  const deferredSearch = useDeferredValue(search)
  const [sort, setSort] = useState<FileSort>('createdAt')
  const [direction, setDirection] = useState<SortDirection>('desc')
  const [page, setPage] = useState(0)
  const [detailId, setDetailId] = useState<string | null>(null)
  const [deleteTarget, setDeleteTarget] = useState<FileSummary | null>(null)

  const toast = useToast()
  const filesQuery = useFiles({
    page,
    size: 10,
    search: deferredSearch.trim() || undefined,
    sort,
    direction,
  })
  const fileDetail = useFile(detailId ?? undefined)
  const download = useDownloadFile()
  const remove = useDeleteFile()

  useEffect(() => {
    setPage(0)
  }, [deferredSearch, sort, direction])

  const handleDownload = (file: FileSummary) => {
    download.mutate(file.id, {
      onSuccess: () => toast.success(`Téléchargement de « ${file.originalName} » prêt.`),
      onError: (error) => toast.error(getApiErrorMessage(error)),
    })
  }

  const handleConfirmDelete = () => {
    if (!deleteTarget) return
    remove.mutate(deleteTarget.id, {
      onSuccess: () => {
        toast.success(`« ${deleteTarget.originalName} » a été supprimé.`)
        if (detailId === deleteTarget.id) setDetailId(null)
        setDeleteTarget(null)
      },
      onError: (error) => toast.error(getApiErrorMessage(error)),
    })
  }

  const files = filesQuery.data?.content ?? []
  const totalSize = files.reduce((sum, file) => sum + file.fileSize, 0)

  return (
    <div className="space-y-6">
      <div className="flex flex-col justify-between gap-3 sm:flex-row sm:items-end">
        <div>
          <p className="text-xs font-semibold uppercase tracking-widest text-brand-600">
            Bibliothèque
          </p>
          <h1 className="mt-1 text-2xl font-bold tracking-tight text-slate-900">Mes fichiers</h1>
          <p className="mt-1 text-sm text-slate-500">
            {filesQuery.data
              ? `${filesQuery.data.totalElements} fichier${
                  filesQuery.data.totalElements > 1 ? 's' : ''
                } · ${formatBytes(totalSize)} affichés`
              : 'Importez et protégez vos documents.'}
          </p>
        </div>
      </div>

      <Card className="p-5">
        <div className="mb-3 flex items-center gap-2">
          <span className="flex h-9 w-9 items-center justify-center rounded-xl bg-brand-50 text-brand-600">
            <UploadCloud className="h-4 w-4" />
          </span>
          <div>
            <p className="text-sm font-semibold text-slate-900">Ajouter un fichier</p>
            <p className="text-xs text-slate-500">
              Le fichier sera stocké de manière sécurisée et prêt à être sauvegardé.
            </p>
          </div>
        </div>
        <FileUploader onUploaded={() => setPage(0)} />
      </Card>

      <Card>
        <div className="flex flex-col gap-3 border-b border-slate-100 p-4 sm:flex-row sm:items-center">
          <div className="relative flex-1">
            <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
            <Input
              value={search}
              onChange={(event) => setSearch(event.target.value)}
              placeholder="Rechercher un fichier…"
              className="pl-9"
              aria-label="Rechercher un fichier"
            />
          </div>
          <div className="flex items-center gap-2">
            <Select
              value={sort}
              onChange={(event) => setSort(event.target.value as FileSort)}
              aria-label="Trier par"
              className="w-40"
            >
              <option value="createdAt">Date d’import</option>
              <option value="originalName">Nom</option>
              <option value="fileSize">Taille</option>
            </Select>
            <Button
              variant="outline"
              size="icon"
              onClick={() => setDirection((value) => (value === 'asc' ? 'desc' : 'asc'))}
              aria-label={direction === 'asc' ? 'Tri croissant' : 'Tri décroissant'}
              title={direction === 'asc' ? 'Croissant' : 'Décroissant'}
            >
              {direction === 'asc' ? (
                <ArrowUpWideNarrow className="h-4 w-4" />
              ) : (
                <ArrowDownWideNarrow className="h-4 w-4" />
              )}
            </Button>
          </div>
        </div>

        {files.length === 0 && !filesQuery.isLoading ? (
          <div className="p-5">
            <EmptyState
              icon={FolderOpen}
              title={deferredSearch ? 'Aucun résultat' : 'Votre bibliothèque est vide'}
              description={
                deferredSearch
                  ? 'Essayez un autre mot-clé ou modifiez vos filtres.'
                  : 'Importez votre premier fichier pour lancer une sauvegarde.'
              }
              action={
                deferredSearch ? (
                  <Button variant="outline" onClick={() => setSearch('')}>
                    Réinitialiser la recherche
                  </Button>
                ) : null
              }
            />
          </div>
        ) : (
          <FileList
            files={files}
            isLoading={filesQuery.isLoading}
            downloadingId={download.isPending ? (download.variables ?? null) : null}
            onView={(file) => setDetailId(file.id)}
            onDownload={handleDownload}
            onDelete={(file) => setDeleteTarget(file)}
          />
        )}
        <div className="px-4 pb-4">
          <Pagination
            page={filesQuery.data?.currentPage ?? page}
            totalPages={filesQuery.data?.totalPages ?? 0}
            totalElements={filesQuery.data?.totalElements ?? 0}
            onPageChange={setPage}
          />
        </div>
      </Card>

      <Modal
        open={Boolean(detailId)}
        onOpenChange={(open) => {
          if (!open) setDetailId(null)
        }}
        title={fileDetail.data?.originalName ?? 'Détails du fichier'}
        description="Métadonnées et empreinte d’intégrité."
        footer={
          fileDetail.data ? (
            <>
              <Button variant="outline" size="sm" onClick={() => setDeleteTarget(fileDetail.data)}>
                Supprimer
              </Button>
              <ManualBackupButton
                fileId={fileDetail.data.id}
                fileName={fileDetail.data.originalName}
                variant="primary"
                size="sm"
              />
            </>
          ) : null
        }
      >
        {fileDetail.isLoading ? (
          <FileMetadataSkeleton />
        ) : fileDetail.isError ? (
          <p className="text-sm text-red-600">{getApiErrorMessage(fileDetail.error)}</p>
        ) : fileDetail.data ? (
          <FileMetadata file={fileDetail.data} />
        ) : null}
      </Modal>

      <ConfirmDialog
        open={Boolean(deleteTarget)}
        onOpenChange={(open) => {
          if (!open) setDeleteTarget(null)
        }}
        title="Supprimer ce fichier ?"
        description={
          deleteTarget
            ? `« ${deleteTarget.originalName} » sera retiré de votre bibliothèque. L’historique de sauvegarde est conservé.`
            : ''
        }
        confirmLabel="Supprimer"
        isLoading={remove.isPending}
        onConfirm={handleConfirmDelete}
      />
    </div>
  )
}
