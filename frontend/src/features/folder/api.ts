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

export async function getFolderTree(): Promise<FolderTreeNode[]> {
  const response = await apiClient.get<ApiResponse<FolderTreeNode[]>>('/api/folders/tree')
  return response.data.data
}
