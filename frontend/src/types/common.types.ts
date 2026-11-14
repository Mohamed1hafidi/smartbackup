export interface ApiResponse<T> {
  data: T
  message?: string
}

export interface ApiError {
  timestamp?: string
  status: number
  error?: string
  message: string
  path?: string
  details?: string[]
}

export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  currentPage: number
  size: number
}

export interface PaginationParams {
  page?: number
  size?: number
}
