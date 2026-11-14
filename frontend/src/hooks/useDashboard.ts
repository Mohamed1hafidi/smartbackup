import { useQuery } from '@tanstack/react-query'
import { dashboardApi } from '../api/dashboardApi'

export const dashboardKeys = {
  stats: ['dashboard', 'stats'] as const,
}

export function useDashboard() {
  return useQuery({
    queryKey: dashboardKeys.stats,
    queryFn: dashboardApi.stats,
  })
}
