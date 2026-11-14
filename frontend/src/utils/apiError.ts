import axios from 'axios'
import type { ApiError } from '../types/common.types'

export function getApiErrorMessage(error: unknown, fallback = 'Une erreur est survenue.'): string {
  if (axios.isAxiosError(error)) {
    const data = error.response?.data as ApiError | undefined
    if (data?.details?.length) {
      return data.details.join(' · ')
    }
    if (data?.message) {
      return data.message
    }
    if (!error.response) {
      return 'Impossible de contacter le serveur. Vérifiez votre connexion.'
    }
  }

  if (error instanceof Error && error.message) {
    return error.message
  }

  return fallback
}
