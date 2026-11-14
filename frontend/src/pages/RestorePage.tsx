import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { FolderOpen, History, RotateCcw, ShieldAlert } from 'lucide-react'
import { BackupDetails } from '../components/backups/BackupDetails'
import { BackupVersionList } from '../components/backups/BackupVersionList'
import { RestoreJobStatus } from '../components/backups/RestoreJobStatus'
import { Button } from '../components/ui/Button'
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/Card'
import { ConfirmDialog } from '../components/ui/ConfirmDialog'
import { EmptyState } from '../components/ui/EmptyState'
import { Select } from '../components/ui/Input'
import { Skeleton } from '../components/ui/Skeleton'
import { useBackup, useBackups, useRestoreBackup } from '../hooks/useBackups'
import { useFiles } from '../hooks/useFiles'
import { useToast } from '../components/ui/Toast'
import type { BackupSummary } from '../types/backup.types'
import { getApiErrorMessage } from '../utils/apiError'
import { formatDate } from '../utils/format'

export function RestorePage() {
  const [fileId, setFileId] = useState('')
  const [selected, setSelected] = useState<BackupSummary | null>(null)
  const [confirmOpen, setConfirmOpen] = useState(false)
  const [restoreJobId, setRestoreJobId] = useState<string | null>(null)

  const toast = useToast()
  const files = useFiles({ page: 0, size: 100 })
  const backups = useBackups({
    page: 0,
    size: 50,
    status: 'SUCCESS',
    ...(fileId ? { fileId } : {}),
  })
  const detail = useBackup(selected?.id)
  const restore = useRestoreBackup()

  useEffect(() => {
    setSelected(null)
    setRestoreJobId(null)
  }, [fileId])

  const versions = fileId ? (backups.data?.content ?? []) : []

  const handleRestore = () => {
    if (!selected) return
    restore.mutate(selected.id, {
      onSuccess: (job) => {
        setRestoreJobId(job.restoreJobId)
        setConfirmOpen(false)
        toast.success('Restauration démarrée.')
      },
      onError: (error) => toast.error(getApiErrorMessage(error)),
    })
  }

  return (
    <div className="space-y-6">
      <div>
        <p className="text-xs font-semibold uppercase tracking-widest text-brand-600">
          Récupération
        </p>
        <h1 className="mt-1 text-2xl font-bold tracking-tight text-slate-900">Restaurer un fichier</h1>
        <p className="mt-1 text-sm text-slate-500">
          Choisissez un fichier puis la version à restaurer. Une sauvegarde de sécurité est créée
          avant chaque restauration.
        </p>
      </div>

      <div className="grid gap-4 lg:grid-cols-5">
        <Card className="lg:col-span-2">
          <CardHeader>
            <CardTitle>1. Choisir un fichier</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <Select
              value={fileId}
              onChange={(event) => setFileId(event.target.value)}
              aria-label="Choisir un fichier"
            >
              <option value="">Sélectionner un fichier…</option>
              {(files.data?.content ?? []).map((file) => (
                <option key={file.id} value={file.id}>
                  {file.originalName}
                </option>
              ))}
            </Select>

            {!files.isLoading && (files.data?.content.length ?? 0) === 0 ? (
              <EmptyState
                icon={FolderOpen}
                title="Aucun fichier disponible"
                description="Importez d’abord un fichier pour pouvoir le restaurer."
                action={
                  <Link
                    to="/files"
                    className="inline-flex h-9 items-center rounded-xl bg-brand-600 px-4 text-xs font-semibold text-white"
                  >
                    Importer un fichier
                  </Link>
                }
                className="py-8"
              />
            ) : null}

            {fileId ? (
              <div className="space-y-2">
                <div className="flex items-center justify-between">
                  <p className="text-xs font-semibold uppercase tracking-wide text-slate-400">
                    2. Choisir une version
                  </p>
                  <span className="text-xs text-slate-400">
                    {versions.length} version{versions.length > 1 ? 's' : ''}
                  </span>
                </div>
                {backups.isLoading ? (
                  <div className="space-y-2">
                    {Array.from({ length: 3 }).map((_, index) => (
                      <Skeleton key={index} className="h-16 w-full" />
                    ))}
                  </div>
                ) : versions.length === 0 ? (
                  <EmptyState
                    icon={History}
                    title="Aucune version réussie"
                    description="Lancez une sauvegarde de ce fichier avant de pouvoir le restaurer."
                    action={
                      <Link
                        to="/files"
                        className="inline-flex h-9 items-center rounded-xl border border-slate-300 bg-white px-4 text-xs font-semibold text-slate-700"
                      >
                        Aller aux fichiers
                      </Link>
                    }
                    className="py-6"
                  />
                ) : (
                  <BackupVersionList
                    backups={versions}
                    selectedId={selected?.id ?? null}
                    onSelect={setSelected}
                  />
                )}
              </div>
            ) : null}
          </CardContent>
        </Card>

        <Card className="lg:col-span-3">
          <CardHeader>
            <CardTitle>3. Vérifier et restaurer</CardTitle>
          </CardHeader>
          <CardContent>
            {!selected ? (
              <EmptyState
                icon={RotateCcw}
                title="Sélectionnez une version"
                description="Les détails de la sauvegarde et l’action de restauration apparaîtront ici."
                className="border-0 bg-transparent py-10"
              />
            ) : (
              <div className="space-y-4">
                <div className="rounded-xl border border-slate-200 bg-slate-50 p-4">
                  <p className="text-sm font-semibold text-slate-900">
                    {selected.fileName} · v{selected.versionNumber}
                  </p>
                  <p className="mt-1 text-xs text-slate-500">
                    Sauvegardée le {formatDate(selected.createdAt)} · version{' '}
                    {selected.backupType === 'MANUAL' ? 'manuelle' : 'planifiée'}
                  </p>
                </div>

                <div className="flex items-start gap-3 rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-800">
                  <ShieldAlert className="mt-0.5 h-5 w-5 shrink-0" />
                  <p>
                    Le fichier actuel sera remplacé par cette version. Une nouvelle sauvegarde de
                    sécurité sera automatiquement créée pour préserver l’historique.
                  </p>
                </div>

                {detail.isLoading ? (
                  <Skeleton className="h-40 w-full" />
                ) : detail.data ? (
                  <BackupDetails backup={detail.data} />
                ) : null}

                <Button
                  size="lg"
                  className="w-full"
                  onClick={() => setConfirmOpen(true)}
                  disabled={selected.status !== 'SUCCESS' || restore.isPending}
                >
                  <RotateCcw className="h-4 w-4" />
                  Restaurer cette version
                </Button>

                {restoreJobId ? <RestoreJobStatus jobId={restoreJobId} /> : null}
              </div>
            )}
          </CardContent>
        </Card>
      </div>

      <ConfirmDialog
        open={confirmOpen}
        onOpenChange={setConfirmOpen}
        title="Confirmer la restauration"
        description={
          selected
            ? `Restaurer « ${selected.fileName} » dans sa version v${selected.versionNumber} ? Le contenu actuel sera remplacé.`
            : ''
        }
        confirmLabel="Restaurer"
        variant="primary"
        isLoading={restore.isPending}
        onConfirm={handleRestore}
      />
    </div>
  )
}
