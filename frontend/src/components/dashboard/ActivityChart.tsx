import { useMemo } from 'react'
import {
  Bar,
  BarChart,
  CartesianGrid,
  Legend,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import { format, isSameDay, parseISO, subDays } from 'date-fns'
import { fr } from 'date-fns/locale'
import type { BackupSummary } from '../../types/backup.types'
import { Card, CardContent, CardHeader, CardTitle } from '../ui/Card'

export function ActivityChart({ backups }: { backups: BackupSummary[] }) {
  const data = useMemo(() => {
    const today = new Date()
    return Array.from({ length: 7 }, (_, index) => {
      const day = subDays(today, 6 - index)
      const dayBackups = backups.filter((backup) => {
        const created = parseISO(backup.createdAt)
        return isSameDay(created, day)
      })

      return {
        day: format(day, 'EEE', { locale: fr }),
        fullDate: format(day, 'dd MMM', { locale: fr }),
        réussies: dayBackups.filter((backup) => backup.status === 'SUCCESS').length,
        échouées: dayBackups.filter((backup) => backup.status === 'FAILED').length,
      }
    })
  }, [backups])

  return (
    <Card>
      <CardHeader>
        <div>
          <CardTitle>Activité des sauvegardes</CardTitle>
          <p className="text-xs text-slate-500">7 derniers jours</p>
        </div>
      </CardHeader>
      <CardContent className="pt-2">
        <div className="h-64 w-full">
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={data} margin={{ top: 8, right: 4, left: -20, bottom: 0 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" vertical={false} />
              <XAxis
                dataKey="day"
                axisLine={false}
                tickLine={false}
                tick={{ fontSize: 11, fill: '#64748b' }}
              />
              <YAxis
                allowDecimals={false}
                axisLine={false}
                tickLine={false}
                tick={{ fontSize: 11, fill: '#94a3b8' }}
              />
              <Tooltip
                cursor={{ fill: 'rgba(37, 99, 235, 0.06)' }}
                contentStyle={{
                  borderRadius: 12,
                  border: '1px solid #e2e8f0',
                  fontSize: 12,
                  boxShadow: '0 10px 30px -12px rgba(15, 23, 42, 0.25)',
                }}
                labelFormatter={(_, payload) =>
                  payload?.[0]?.payload?.fullDate ?? ''
                }
              />
              <Legend
                iconType="circle"
                iconSize={8}
                wrapperStyle={{ fontSize: 12, paddingTop: 8 }}
              />
              <Bar dataKey="réussies" fill="#2563eb" radius={[4, 4, 0, 0]} maxBarSize={28} />
              <Bar dataKey="échouées" fill="#f87171" radius={[4, 4, 0, 0]} maxBarSize={28} />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </CardContent>
    </Card>
  )
}
