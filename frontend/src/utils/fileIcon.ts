import {
  Archive,
  File,
  FileCode2,
  FileImage,
  FileSpreadsheet,
  FileText,
  type LucideIcon,
} from 'lucide-react'

export function fileIcon(mimeType: string): LucideIcon {
  if (mimeType.startsWith('image/')) return FileImage
  if (mimeType === 'application/pdf') return FileText
  if (mimeType.includes('zip') || mimeType.includes('archive')) return Archive
  if (mimeType.includes('json') || mimeType.includes('text')) return FileCode2
  if (mimeType.includes('sheet') || mimeType.includes('csv')) return FileSpreadsheet
  return File
}

export function fileTypeLabel(mimeType: string): string {
  if (mimeType.startsWith('image/')) return 'Image'
  if (mimeType === 'application/pdf') return 'PDF'
  if (mimeType.includes('zip')) return 'Archive'
  if (mimeType.includes('json')) return 'JSON'
  if (mimeType.startsWith('text/')) return 'Texte'
  return mimeType.split('/').pop()?.toUpperCase() ?? 'Fichier'
}
