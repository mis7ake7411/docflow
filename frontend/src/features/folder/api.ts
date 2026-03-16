import { apiClient } from '@/shared/api/axios'

export interface ApiResponse<T> {
  success: boolean
  data: T
  message: string | null
}

export interface FolderTreeNode {
  id: number
  name: string
  parentId: number | null
  sortOrder: number
  createdBy: number
  createdAt: string
  updatedAt: string
  children: FolderTreeNode[]
}

export interface FolderPayload {
  name: string
  parentId: number | null
  sortOrder: number
}

export async function getFolderTree(): Promise<FolderTreeNode[]> {
  const response = await apiClient.get<ApiResponse<FolderTreeNode[]>>('/api/folders/tree')
  return response.data.data
}

export async function createFolder(payload: FolderPayload): Promise<FolderTreeNode> {
  const response = await apiClient.post<ApiResponse<FolderTreeNode>>('/api/folders', payload)
  return response.data.data
}

export async function updateFolder(id: number, payload: FolderPayload): Promise<FolderTreeNode> {
  const response = await apiClient.put<ApiResponse<FolderTreeNode>>(`/api/folders/${id}`, payload)
  return response.data.data
}

export async function deleteFolder(id: number): Promise<void> {
  await apiClient.delete(`/api/folders/${id}`)
}
