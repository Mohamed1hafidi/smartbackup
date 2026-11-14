import { useEffect, useState } from 'react'
import { History, RefreshCw } from 'lucide-react'
import { BackupDetails } from '../components/backups/BackupDetails'
import { BackupHistory } from '../components/backups/BackupHistory'
import { RestoreJobStatus } from '../components/backups/RestoreJobStatus'
import { Button } from '../components/ui/Button'
import { Card } from '../components/ui/Card'
import { ConfirmDialog } from '../components/ui/ConfirmDialog'
import { EmptyState } from '../components/ui/EmptyState'
import { Select } from '../components/ui/Input'
import { Modal } from '../components/ui/Modal'
import { Pagination } from '../components/ui/Pagination'
import { Skeleton } from '../components/ui/Skeleton'
import { useBackup, useBackups, useDownloadBackup, useRestoreBackup } from '../hooks/useBackups'
import { useFiles } from '../hooks/useFiles'
import { useToast } from '../components/ui/Toast'
import type { BackupStatus, BackupSummary } from '../types/backup.types'
import { getApiErrorMessage } from '../utils/apiError'

const STATUS_OPTIONS: { value: '' | BackupStatus; label: string }[] = [
  { value: '', label: 'Tous les statuts' },
  { value: 'SUCCESS', label: 'Réussies' },
  { value: 'FAILED', label: 'Échouées' },
  { value: 'PENDING', label: 'En attente' },
  { value: 'IN_PROGRESS', label: 'En cours' },
]

export function BackupsPage() {
  const [status, setStatus] = useState<'' | BackupStatus>('')
  const [fileId, setFileId] = useState('')
  const [page, setPage] = useState(0)
  const [detailId, setDetailId] = useState<string | null>(null)
  const [restoreTarget, setRestoreTarget] = useState<BackupSummary | null>(null)
  const [restoreJobId, setRestoreJobId] = useState<string | null>(null)

  const toast = useToast()
  const files = useFiles({ page: 0, size: 100 })
  const backups = useBackups({
    page,
    size: 10,
    ...(status ? { status } : {}),
    ...(fileId ? { fileId } : {}),
  })
  const detail = useBackup(detailId ?? undefined)
  const download = useDownloadBackup()
  const restore = useRestoreBackup()

  useEffect(() => {
    setPage(0)
  }, [status, fileId])

  const handleDownload = (backup: BackupSummary) => {
    download.mutate(backup.id, {
      onSuccess: () => toast.success(`Téléchargement de « ${backup.fileName} » prêt.`),
      onError: (error) => toast.error(getApiErrorMessage(error)),
    })
  }

  const handleConfirmRestore = () => {
    if (!restoreTarget) return
    restore.mutate(restoreTarget.id, {
      onSuccess: (job) => {
        setRestoreJobId(job.restoreJobId)
        setRestoreTarget(null)
        toast.success('Restauration démarrée. Vous pouvez suivre son état.')
      },
      onError: (error) => toast.error(getApiErrorMessage(error)),
    })
  }

  const items = backups.data?.content ?? []
  const hasFilters = Boolean(status || fileId)

  return (
    <div className="space-y-6">
      <div className="flex flex-col justify-between gap-3 sm:flex-row sm:items-end">
        <div>
          <p className="text-xs font-semibold uppercase tracking-widest text-brand-600">
            Historique
          </p>
          <h1 className="mt-1 text-2xl font-bold tracking-tight text-slate-900">
            Mes sauvegardes
          </h1>
          <p className="mt-1 text-sm text-slate-500">
            {backups.data
              ? `${backups.data.totalElements} sauvegarde${
                  backups.data.totalElements > 1 ? 's' : ''
                } au total`
              : 'Toutes vos versions sont conservées ici.'}
          </p>
        </div>
        <Button
          variant="outline"
          onClick={() => void backups.refetch()}
          disabled={backups.isFetching}
        >
          <RefreshCw className={`h-4 w-4 ${backups.isFetching ? 'animate-spin' : ''}`} />
          Actualiser
        </Button>
      </div>

      <Card>
        <div className="flex flex-col gap-3 border-b border-slate-100 p-4 sm:flex-row">
          <Select
            value={status}
            onChange={(event) => setStatus(event.target.value as '' | BackupStatus)}
            aria-label="Filtrer par statut"
            className="sm:w-48"
          >
            {STATUS_OPTIONS.map((option) => (
              <option key={option.value || 'all'} value={option.value}>
                {option.label}
              </option>
            ))}
          </Select>
          <Select
            value={fileId}
            onChange={(event) => setFileId(event.target.value)}
            aria-label="Filtrer par fichier"
            className="sm:w-64"
          >
            <option value="">Tous les fichiers</option>
            {(files.data?.content ?? []).map((file) => (
              <option key={file.id} value={file.id}>
                {file.originalName}
              </option>
            ))}
          </Select>
          {hasFilters ? (
            <Button
              variant="ghost"
              size="sm"
              className="sm:ml-auto"
              onClick={() => {
                setStatus('')
                setFileId('')
              }}
            >
              Réinitialiser
            </Button>
          ) : null}
        </div>

        {backups.isError ? (
          <div className="flex flex-col items-start justify-between gap-3 border-b border-red-100 bg-red-50 p-4 sm:flex-row sm:items-center">
            <p className="text-sm text-red-700">{getApiErrorMessage(backups.error)}</p>
            <Button variant="outline" size="sm" onClick={() => void backups.refetch()}>
              Réessayer
            </Button>
          </div>
        ) : null}

        {items.length === 0 && !backups.isLoading ? (
          <div className="p-5">
            <EmptyState
              icon={History}
              title={hasFilters ? 'Aucune sauvegarde trouvée' : 'Aucune sauvegarde pour le moment'}
              description={
                hasFilters
                  ? 'Modifiez vos filtres pour voir d’autres résultats.'
                  : 'Importez un fichier puis lancez une sauvegarde manuelle ou planifiée.'
              }
            />
          </div>
        ) : (
          <BackupHistory
            backups={items}
            isLoading={backups.isLoading}
            downloadingId={download.isPending ? (download.variables ?? null) : null}
            onView={(backup) => setDetailId(backup.id)}
            onDownload={handleDownload}
            onRestore={(backup) => {
              setDetailId(null)
              setRestoreTarget(backup)
            }}
          />
        )}

        <div className="px-4 pb-4">
          <Pagination
            page={backups.data?.currentPage ?? page}
            totalPages={backups.data?.totalPages ?? 0}
            totalElements={backups.data?.totalElements ?? 0}
            onPageChange={setPage}
          />
        </div>
      </Card>

      <Modal
        open={Boolean(detailId)}
        onOpenChange={(open) => {
          if (!open) setDetailId(null)
        }}
        title="Détail de la sauvegarde"
        description="Informations complètes et intégrité du fichier."
        footer={
          detail.data ? (
            <>
              <Button
                variant="outline"
                size="sm"
                onClick={() => handleDownload(detail.data)}
                disabled={download.isPending}
              >
                Télécharger
              </Button>
              <Button
                size="sm"
                onClick={() => {
                  setDetailId(null)
                  setRestoreTarget(detail.data)
                }}
                disabled={detail.data.status !== 'SUCCESS'}
              >
                Restaurer cette version
              </Button>
            </>
          ) : null
        }
      >
        {detail.isLoading ? (
          <div className="space-y-3">
            <Skeleton className="h-6 w-32" />
            <Skeleton className="h-24 w-full" />
            <Skeleton className="h-16 w-full" />
          </div>
        ) : detail.isError ? (
          <p className="text-sm text-red-600">{getApiErrorMessage(detail.error)}</p>
        ) : detail.data ? (
          <BackupDetails backup={detail.data} />
        ) : null}
      </Modal>

      <ConfirmDialog
        open={Boolean(restoreTarget)}
        onOpenChange={(open) => {
          if (!open) setRestoreTarget(null)
        }}
        title="Restaurer cette version ?"
        description={
          restoreTarget
            ? `La version v${restoreTarget.versionNumber} de « ${restoreTarget.fileName} » remplacera le fichier actuel. Une sauvegarde de sécurité sera créée automatiquement.`
            : ''
        }
        confirmLabel="Restaurer"
        variant="primary"
        isLoading={restore.isPending}
        onConfirm={handleConfirmRestore}
      />

      <Modal
        open={Boolean(restoreJobId)}
        onOpenChange={(open) => {
          if (!open) setRestoreJobId(null)
        }}
        title="Restauration"
        description="Suivi de votre demande de restauration."
        footer={
          <Button variant="outline" onClick={() => setRestoreJobId(null)}>
            Fermer
          </Button>
        }
      >
        {restoreJobId ? <RestoreJobStatus jobId={restoreJobId} /> : null}
      </Modal>
    </div>
  )
}
