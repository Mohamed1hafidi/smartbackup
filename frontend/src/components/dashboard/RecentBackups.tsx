import { ArrowRight, History } from 'lucide-react'
import { Link } from 'react-router-dom'
import type { BackupSummary } from '../../types/backup.types'
import { formatBytes, formatDate } from '../../utils/format'
import { Card, CardContent, CardHeader, CardTitle } from '../ui/Card'
import { EmptyState } from '../ui/EmptyState'
import { StatusBadge } from '../ui/StatusBadge'

export function RecentBackups({ backups }: { backups: BackupSummary[] }) {
  return (
    <Card>
      <CardHeader className="items-center">
        <div>
          <CardTitle>Dernières sauvegardes</CardTitle>
          <p className="text-xs text-slate-500">Les activités les plus récentes</p>
        </div>
        <Link
          to="/backups"
          className="inline-flex items-center gap-1 text-xs font-semibold text-brand-700 hover:text-brand-800"
        >
          Tout voir
          <ArrowRight className="h-3.5 w-3.5" />
        </Link>
      </CardHeader>
      <CardContent className="pt-2">
        {backups.length === 0 ? (
          <EmptyState
            icon={History}
            title="Aucune sauvegarde pour le moment"
            description="Importez un fichier puis lancez votre première sauvegarde."
            className="border-0 bg-transparent py-8"
          />
        ) : (
          <ul className="divide-y divide-slate-100">
            {backups.map((backup) => (
              <li key={backup.id} className="flex items-center gap-3 py-3">
                <span className="flex h-9 w-9 items-center justify-center rounded-xl bg-slate-100 text-slate-500">
                  <History className="h-4 w-4" />
                </span>
                <div className="min-w-0 flex-1">
                  <p className="truncate text-sm font-medium text-slate-800">{backup.fileName}</p>
                  <p className="text-xs text-slate-400">
                    v{backup.versionNumber} · {formatBytes(backup.fileSize)} ·{' '}
                    {formatDate(backup.createdAt, 'dd MMM, HH:mm')}
                  </p>
                </div>
                <StatusBadge status={backup.status} />
              </li>
            ))}
          </ul>
        )}
      </CardContent>
    </Card>
  )
}
