import { CheckCircle2, Loader2, XCircle } from 'lucide-react'
import { useRestoreStatus } from '../../hooks/useBackups'
import { formatDate } from '../../utils/format'
import { StatusBadge } from '../ui/StatusBadge'

export function RestoreJobStatus({ jobId }: { jobId: string }) {
  const query = useRestoreStatus(jobId)
  const job = query.data

  return (
    <div className="space-y-3">
      <div className="rounded-xl bg-slate-50 p-4">
        <div className="flex items-center gap-3">
          <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-white text-brand-600 shadow-sm">
            {job?.status === 'SUCCESS' ? (
              <CheckCircle2 className="h-5 w-5 text-emerald-600" />
            ) : job?.status === 'FAILED' ? (
              <XCircle className="h-5 w-5 text-red-600" />
            ) : (
              <Loader2 className="h-5 w-5 animate-spin" />
            )}
          </span>
          <div>
            <p className="text-sm font-semibold text-slate-900">
              {job?.status === 'SUCCESS'
                ? 'Restauration terminée'
                : job?.status === 'FAILED'
                  ? 'Restauration échouée'
                  : 'Restauration en cours…'}
            </p>
            <p className="text-xs text-slate-500">
              {job ? `Démarrée le ${formatDate(job.initiatedAt)}` : 'Initialisation…'}
            </p>
          </div>
          {job ? <StatusBadge status={job.status} className="ml-auto" /> : null}
        </div>
      </div>

      {job?.errorMessage ? (
        <p className="rounded-xl border border-red-200 bg-red-50 p-3 text-sm text-red-700">
          {job.errorMessage}
        </p>
      ) : null}

      <p className="text-xs text-slate-500">
        L’état est actualisé automatiquement toutes les 2 secondes.
      </p>
    </div>
  )
}
