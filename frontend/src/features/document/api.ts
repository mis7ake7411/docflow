import { apiClient } from '@/shared/api/axios'

export interface ApiResponse<T> {
  success: boolean
  data: T
  message: string | null
}

export interface DocumentItem {
  id: number
  folderId: number | null
  title: string
  description: string | null
  fileName: string | null
  storedFileName: string | null
  contentType: string | null
  fileSize: number | null
  version: number
  status: string
  createdBy: number
  createdAt: string
  updatedAt: string
}

export async function getDocuments(): Promise<DocumentItem[]> {
  const response = await apiClient.get<ApiResponse<DocumentItem[]>>('/api/documents')
  return response.data.data
}
