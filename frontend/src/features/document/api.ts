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

export interface CreateDocumentRequest {
  folderId: number | null
  title: string
  description: string
  status: string
}

export interface UpdateDocumentRequest {
  folderId: number | null
  title: string
  description: string
  status: string
}

export async function getDocuments(): Promise<DocumentItem[]> {
  const response = await apiClient.get<ApiResponse<DocumentItem[]>>('/api/documents')
  return response.data.data
}

export async function getDocumentDetail(id: number): Promise<DocumentItem> {
  const response = await apiClient.get<ApiResponse<DocumentItem>>(`/api/documents/${id}`)
  return response.data.data
}

export async function createDocument(request: CreateDocumentRequest): Promise<DocumentItem> {
  const response = await apiClient.post<ApiResponse<DocumentItem>>('/api/documents', request)
  return response.data.data
}

export async function updateDocument(id: number, request: UpdateDocumentRequest): Promise<DocumentItem> {
  const response = await apiClient.put<ApiResponse<DocumentItem>>(`/api/documents/${id}`, request)
  return response.data.data
}

export async function deleteDocument(id: number): Promise<void> {
  await apiClient.delete(`/api/documents/${id}`)
}

export async function uploadDocumentFile(id: number, file: File): Promise<DocumentItem> {
  const formData = new FormData()
  formData.append('file', file)

  const response = await apiClient.post<ApiResponse<DocumentItem>>(`/api/documents/${id}/upload`, formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  })

  return response.data.data
}

export async function downloadDocumentFile(id: number): Promise<Blob> {
  const response = await apiClient.get(`/api/documents/${id}/download`, {
    responseType: 'blob',
  })
  return response.data
}
