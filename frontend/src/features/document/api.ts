import { apiClient } from '@/shared/api/axios'

export interface ApiResponse<T> {
  success: boolean
  data: T
  message: string | null
}

export interface PagedResponse<T> {
  items: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export type DocumentAccessLevel = 'OWNER' | 'VIEW' | 'EDIT' | 'ADMIN'

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
  accessLevel: DocumentAccessLevel | null
  sharedBy: string | null
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

export interface DocumentShareItem {
  id: number
  documentId: number
  userId: number
  username: string
  email: string
  permission: Extract<DocumentAccessLevel, 'VIEW' | 'EDIT'>
  sharedBy: string | null
  createdAt: string
}

export interface ShareDocumentRequest {
  sharedWithUserId: number
  permission: Extract<DocumentAccessLevel, 'VIEW' | 'EDIT'>
}

function emptyPage<T>(size: number): PagedResponse<T> {
  return {
    items: [],
    page: 0,
    size,
    totalElements: 0,
    totalPages: 0,
  }
}

export async function getDocuments(page = 0, size = 10, folderId?: number | null): Promise<PagedResponse<DocumentItem>> {
  const response = await apiClient.get<ApiResponse<PagedResponse<DocumentItem>>>('/api/documents', {
    params: {
      page,
      size,
      folderId: folderId ?? undefined,
    },
  })
  return response.data.data ?? emptyPage<DocumentItem>(size)
}

export async function getSharedDocuments(page = 0, size = 10): Promise<PagedResponse<DocumentItem>> {
  const response = await apiClient.get<ApiResponse<PagedResponse<DocumentItem>>>('/api/documents/shared-with-me', {
    params: {
      page,
      size,
    },
  })
  return response.data.data ?? emptyPage<DocumentItem>(size)
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

export async function getDocumentShares(documentId: number): Promise<DocumentShareItem[]> {
  const response = await apiClient.get<ApiResponse<DocumentShareItem[]>>(`/api/documents/${documentId}/shares`)
  return response.data.data ?? []
}

export async function addDocumentShare(documentId: number, request: ShareDocumentRequest): Promise<DocumentShareItem> {
  const response = await apiClient.post<ApiResponse<DocumentShareItem>>(`/api/documents/${documentId}/shares`, request)
  return response.data.data
}

export async function updateDocumentShare(documentId: number, shareId: number, request: ShareDocumentRequest): Promise<DocumentShareItem> {
  const response = await apiClient.put<ApiResponse<DocumentShareItem>>(`/api/documents/${documentId}/shares/${shareId}`, request)
  return response.data.data
}

export async function deleteDocumentShare(documentId: number, shareId: number): Promise<void> {
  await apiClient.delete(`/api/documents/${documentId}/shares/${shareId}`)
}
