import { useState } from 'react'
import { Link } from 'react-router-dom'
import { CalendarPlus, CalendarClock } from 'lucide-react'
import { ScheduleExecutionList } from '../components/schedules/ScheduleExecutionList'
import { ScheduleForm } from '../components/schedules/ScheduleForm'
import { ScheduleList } from '../components/schedules/ScheduleList'
import { Button } from '../components/ui/Button'
import { Card } from '../components/ui/Card'
import { ConfirmDialog } from '../components/ui/ConfirmDialog'
import { EmptyState } from '../components/ui/EmptyState'
import { Modal } from '../components/ui/Modal'
import { Pagination } from '../components/ui/Pagination'
import { Skeleton } from '../components/ui/Skeleton'
import { useToast } from '../components/ui/Toast'
import { useFiles } from '../hooks/useFiles'
import {
  useDeleteSchedule,
  useScheduleExecutions,
  useSchedules,
  useToggleSchedule,
} from '../hooks/useSchedules'
import type { Schedule } from '../types/schedule.types'
import { getApiErrorMessage } from '../utils/apiError'

export function SchedulesPage() {
  const [formOpen, setFormOpen] = useState(false)
  const [editing, setEditing] = useState<Schedule | null>(null)
  const [deleteTarget, setDeleteTarget] = useState<Schedule | null>(null)
  const [historyTarget, setHistoryTarget] = useState<Schedule | null>(null)
  const [historyPage, setHistoryPage] = useState(0)

  const toast = useToast()
  const schedules = useSchedules()
  const files = useFiles({ page: 0, size: 100 })
  const toggle = useToggleSchedule()
  const remove = useDeleteSchedule()
  const executions = useScheduleExecutions(historyTarget?.id, historyPage)

  const items = schedules.data?.data ?? []
  const activeCount = items.filter((schedule) => schedule.isActive).length

  const handleToggle = (schedule: Schedule) => {
    toggle.mutate(schedule.id, {
      onSuccess: (updated) =>
        toast.success(updated.isActive ? 'Planification activée.' : 'Planification désactivée.'),
      onError: (error) => toast.error(getApiErrorMessage(error)),
    })
  }

  const handleDelete = () => {
    if (!deleteTarget) return
    remove.mutate(deleteTarget.id, {
      onSuccess: () => {
        toast.success('Planification supprimée.')
        setDeleteTarget(null)
      },
      onError: (error) => toast.error(getApiErrorMessage(error)),
    })
  }

  const openCreate = () => {
    setEditing(null)
    setFormOpen(true)
  }

  const openEdit = (schedule: Schedule) => {
    setEditing(schedule)
    setFormOpen(true)
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col justify-between gap-3 sm:flex-row sm:items-end">
        <div>
          <p className="text-xs font-semibold uppercase tracking-widest text-brand-600">
            Automatisation
          </p>
          <h1 className="mt-1 text-2xl font-bold tracking-tight text-slate-900">
            Planifications
          </h1>
          <p className="mt-1 text-sm text-slate-500">
            {items.length > 0
              ? `${items.length} planification${items.length > 1 ? 's' : ''} · ${activeCount} active${
                  activeCount > 1 ? 's' : ''
                }`
              : 'Automatisez la sauvegarde de vos fichiers.'}
          </p>
        </div>
        <Button
          onClick={openCreate}
          disabled={(files.data?.content.length ?? 0) === 0}
          title={
            (files.data?.content.length ?? 0) === 0
              ? 'Importez d’abord un fichier.'
              : 'Créer une planification'
          }
        >
          <CalendarPlus className="h-4 w-4" />
          Nouvelle planification
        </Button>
      </div>

      {schedules.isError ? (
        <Card className="p-4">
          <div className="flex flex-col items-start justify-between gap-3 sm:flex-row sm:items-center">
            <p className="text-sm text-red-700">{getApiErrorMessage(schedules.error)}</p>
            <Button variant="outline" size="sm" onClick={() => void schedules.refetch()}>
              Réessayer
            </Button>
          </div>
        </Card>
      ) : null}

      {schedules.isLoading ? (
        <div className="grid gap-4 md:grid-cols-2">
          {Array.from({ length: 2 }).map((_, index) => (
            <Skeleton key={index} className="h-56 w-full" />
          ))}
        </div>
      ) : items.length === 0 ? (
        <Card className="p-6">
          <EmptyState
            icon={CalendarClock}
            title="Aucune planification"
            description={
              (files.data?.content.length ?? 0) === 0
                ? 'Importez un fichier pour créer votre première sauvegarde automatique.'
                : 'Programmez des sauvegardes quotidiennes ou hebdomadaires en quelques clics.'
            }
            action={
              (files.data?.content.length ?? 0) === 0 ? (
                <Link
                  to="/files"
                  className="inline-flex h-9 items-center rounded-xl bg-brand-600 px-4 text-xs font-semibold text-white"
                >
                  Importer un fichier
                </Link>
              ) : (
                <Button size="sm" onClick={openCreate}>
                  <CalendarPlus className="h-4 w-4" />
                  Créer une planification
                </Button>
              )
            }
          />
        </Card>
      ) : (
        <ScheduleList
          schedules={items}
          togglingId={toggle.isPending ? (toggle.variables ?? null) : null}
          onToggle={handleToggle}
          onEdit={openEdit}
          onDelete={setDeleteTarget}
          onHistory={(schedule) => {
            setHistoryPage(0)
            setHistoryTarget(schedule)
          }}
        />
      )}

      <ScheduleForm
        open={formOpen}
        onOpenChange={setFormOpen}
        files={files.data?.content ?? []}
        schedule={editing}
      />

      <ConfirmDialog
        open={Boolean(deleteTarget)}
        onOpenChange={(open) => {
          if (!open) setDeleteTarget(null)
        }}
        title="Supprimer la planification ?"
        description={
          deleteTarget
            ? `La sauvegarde automatique de « ${deleteTarget.fileName} » sera définitivement arrêtée. L’historique des sauvegardes reste disponible.`
            : ''
        }
        confirmLabel="Supprimer"
        isLoading={remove.isPending}
        onConfirm={handleDelete}
      />

      <Modal
        open={Boolean(historyTarget)}
        onOpenChange={(open) => {
          if (!open) setHistoryTarget(null)
        }}
        title="Journal d’exécution"
        description={
          historyTarget
            ? `Exécutions de « ${historyTarget.fileName} »`
            : undefined
        }
        className="max-w-2xl"
        footer={
          <Button variant="outline" onClick={() => setHistoryTarget(null)}>
            Fermer
          </Button>
        }
      >
        <ScheduleExecutionList
          executions={executions.data?.content ?? []}
          isLoading={executions.isLoading}
        />
        <Pagination
          page={executions.data?.currentPage ?? historyPage}
          totalPages={executions.data?.totalPages ?? 0}
          totalElements={executions.data?.totalElements ?? 0}
          onPageChange={setHistoryPage}
        />
      </Modal>
    </div>
  )
}
