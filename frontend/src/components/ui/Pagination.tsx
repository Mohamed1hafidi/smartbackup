import { ChevronLeft, ChevronRight } from 'lucide-react'
import { Button } from './Button'

interface PaginationProps {
  page: number
  totalPages: number
  totalElements: number
  onPageChange: (page: number) => void
}

export function Pagination({ page, totalPages, totalElements, onPageChange }: PaginationProps) {
  if (totalPages <= 1) {
    return null
  }

  return (
    <div className="flex flex-col items-center justify-between gap-3 pt-4 sm:flex-row">
      <p className="text-xs text-slate-500">
        {totalElements} résultat{totalElements > 1 ? 's' : ''} · page {page + 1} sur {totalPages}
      </p>
      <div className="flex items-center gap-2">
        <Button
          variant="outline"
          size="sm"
          disabled={page <= 0}
          onClick={() => onPageChange(page - 1)}
        >
          <ChevronLeft className="h-4 w-4" />
          Précédent
        </Button>
        <Button
          variant="outline"
          size="sm"
          disabled={page >= totalPages - 1}
          onClick={() => onPageChange(page + 1)}
        >
          Suivant
          <ChevronRight className="h-4 w-4" />
        </Button>
      </div>
    </div>
  )
}
