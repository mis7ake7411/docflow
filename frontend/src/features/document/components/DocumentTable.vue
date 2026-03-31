<template>
  <div class="document-panel">
    <div class="section-header">
      <div>
        <h3>{{ headerTitle }}</h3>
        <p class="muted">{{ sectionDescription }}</p>
      </div>

      <div class="header-actions">
        <el-button v-if="showCreateButton" type="primary" @click="openCreateDialog">新增文件</el-button>
      </div>
    </div>

    <div class="table-container">
      <el-skeleton v-if="isLoading" :rows="8" animated />
      <el-alert v-else-if="error" title="文件清單載入失敗" type="error" show-icon :closable="false" />
      <el-empty v-else-if="!items.length" :description="emptyDescription" />

      <el-table v-else :data="items" stripe>
        <el-table-column prop="title" label="標題" min-width="220" />
        <el-table-column v-if="scope === 'shared'" label="權限" width="120">
          <template #default="scopeSlot">
            {{ getAccessLevelLabel(scopeSlot.row.accessLevel) }}
          </template>
        </el-table-column>
        <el-table-column v-if="scope === 'shared'" label="分享者" min-width="160">
          <template #default="scopeSlot">
            {{ scopeSlot.row.sharedBy || '未知' }}
          </template>
        </el-table-column>
        <el-table-column label="狀態" width="120">
          <template #default="scopeSlot">
            {{ getStatusLabel(scopeSlot.row.status) }}
          </template>
        </el-table-column>
        <el-table-column prop="version" label="版本" width="90" />
        <el-table-column prop="fileName" label="檔名" min-width="160" />
        <el-table-column label="更新時間" min-width="180">
          <template #default="scopeSlot">
            {{ formatDate(scopeSlot.row.updatedAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280">
          <template #default="scopeSlot">
            <el-button text type="primary" @click="openDetail(scopeSlot.row.id)">查看</el-button>

            <template v-if="canEditDocument(scopeSlot.row, currentUser)">
              <el-button text @click="openEditDialog(scopeSlot.row)">編輯</el-button>
              <el-button text @click="openUploadDialog(scopeSlot.row)">上傳</el-button>
            </template>
            <template v-else>
              <el-tooltip :content="getDocumentAccessHint(scopeSlot.row, currentUser)">
                <span><el-button text disabled>編輯</el-button></span>
              </el-tooltip>
              <el-tooltip :content="getDocumentAccessHint(scopeSlot.row, currentUser)">
                <span><el-button text disabled>上傳</el-button></span>
              </el-tooltip>
            </template>

            <template v-if="canDeleteDocument(scopeSlot.row, currentUser)">
              <el-popconfirm title="確定刪除這份文件？" @confirm="handleDelete(scopeSlot.row.id)">
                <template #reference>
                  <el-button text type="danger">刪除</el-button>
                </template>
              </el-popconfirm>
            </template>
            <template v-else>
              <el-tooltip :content="PERMISSION_MESSAGES.documentForbidden">
                <span><el-button text type="danger" disabled>刪除</el-button></span>
              </el-tooltip>
            </template>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div v-if="!isLoading && !error && totalElements" class="pagination">
      <el-pagination
        :current-page="currentPage"
        :page-size="pageSize"
        :page-sizes="pageSizes"
        :total="totalElements"
        layout="total, sizes, prev, pager, next"
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </div>

    <DocumentFormDialog v-model="createDialogVisible" :document="null" />
    <DocumentFormDialog v-model="editDialogVisible" :document="editingDocument" />
    <DocumentUploadDialog
      v-if="uploadingDocumentId"
      v-model="uploadDialogVisible"
      :document-id="uploadingDocumentId"
      @update:modelValue="closeUploadDialog"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useMutation, useQuery, useQueryClient } from '@tanstack/vue-query'
import { ElMessage } from 'element-plus'
import { isAxiosError } from 'axios'
import { deleteDocument, getDocuments, getSharedDocuments, type DocumentItem } from '@/features/document/api'
import DocumentFormDialog from '@/features/document/components/DocumentFormDialog.vue'
import DocumentUploadDialog from '@/features/document/components/DocumentUploadDialog.vue'
import { getFolderTree, type FolderTreeNode } from '@/features/folder/api'
import { getAccessLevelLabel, getStatusLabel } from '@/shared/utils/display'
import {
  canDeleteDocument,
  canEditDocument,
  getDocumentAccessHint,
  PERMISSION_MESSAGES,
} from '@/shared/utils/permission'
import { useAuthStore } from '@/stores/auth'
import { useUiStore } from '@/stores/ui'

const props = withDefaults(defineProps<{
  scope?: 'mine' | 'shared'
  headerTitle?: string
  headerDescription?: string
  showCreateButton?: boolean
}>(), {
  scope: 'mine',
  headerTitle: '文件列表',
  headerDescription: '',
  showCreateButton: true,
})

const router = useRouter()
const authStore = useAuthStore()
const uiStore = useUiStore()
const queryClient = useQueryClient()

const createDialogVisible = ref(false)
const editDialogVisible = ref(false)
const editingDocument = ref<DocumentItem | null>(null)
const uploadDialogVisible = ref(false)
const uploadingDocumentId = ref<number | null>(null)
const currentPage = ref(1)
const pageSize = ref(10)
const pageSizes = [10, 20, 50]

const currentUser = computed(() => authStore.user)
const currentUserId = computed(() => currentUser.value?.id ?? null)

const { data: folderTree } = useQuery({
  queryKey: computed(() => ['folders', 'tree', currentUserId.value]),
  queryFn: getFolderTree,
  enabled: computed(() => props.scope === 'mine'),
})

const selectedFolderId = computed(() => uiStore.selectedFolderId)
const selectedFolderIdForQuery = computed(() => {
  if (props.scope !== 'mine') {
    return null
  }

  const folderId = selectedFolderId.value
  if (folderId == null) {
    return null
  }

  if (!uiStore.folderTreeReady || !folderTree.value) {
    return folderId
  }

  return findFolderById(folderTree.value, folderId) ? folderId : null
})

watch(currentUserId, (nextUserId) => {
  if (nextUserId != null && uiStore.folderContextUserId !== nextUserId) {
    uiStore.syncFolderContextForUser(nextUserId)
    currentPage.value = 1
  }
}, { immediate: true })

const query = useQuery({
  queryKey: computed(() => [
    'documents',
    currentUserId.value,
    props.scope,
    currentPage.value,
    pageSize.value,
    selectedFolderIdForQuery.value,
  ]),
  queryFn: () => props.scope === 'shared'
    ? getSharedDocuments(currentPage.value - 1, pageSize.value)
    : getDocuments(currentPage.value - 1, pageSize.value, selectedFolderIdForQuery.value),
})

const { data, isLoading, error } = query

const deleteMutation = useMutation({
  mutationFn: deleteDocument,
  onSuccess: async () => {
    await queryClient.invalidateQueries({ queryKey: ['documents'] })
    ElMessage.success('文件已刪除')
  },
  onError: (mutationError) => {
    if (isAxiosError(mutationError) && mutationError.response?.status === 403) {
      ElMessage.error(PERMISSION_MESSAGES.documentForbidden)
    }
  },
})

const items = computed(() => data.value?.items ?? [])
const totalElements = computed(() => data.value?.totalElements ?? 0)
const selectedFolderName = computed(() => {
  const folderId = selectedFolderIdForQuery.value
  if (!folderId || props.scope !== 'mine' || !uiStore.folderTreeReady || !folderTree.value) return null
  return findFolderName(folderTree.value, folderId)
})

const sectionDescription = computed(() => {
  if (props.scope === 'shared') return props.headerDescription || '顯示其他人分享給你的文件'
  if (!selectedFolderId.value) return '顯示自己的文件'
  if (!uiStore.folderTreeReady) return '正在載入自己的資料夾'
  const name = selectedFolderName.value
  return name ? `目前顯示自己的資料夾「${name}」的文件` : '顯示自己的文件'
})

const emptyDescription = computed(() => props.scope === 'shared' ? '目前沒有分享給你的文件' : '目前沒有自己的文件')

function openDetail(documentId: number) {
  uiStore.setSelectedDocumentId(documentId)
  router.push(`/app/documents/${documentId}`)
}

function openCreateDialog() {
  createDialogVisible.value = true
}

function openEditDialog(document: DocumentItem) {
  editingDocument.value = document
  editDialogVisible.value = true
}

function openUploadDialog(document: DocumentItem) {
  uploadingDocumentId.value = document.id
  uploadDialogVisible.value = true
}

function closeUploadDialog() {
  uploadDialogVisible.value = false
  uploadingDocumentId.value = null
}

async function handleDelete(documentId: number) {
  await deleteMutation.mutateAsync(documentId)
}

function handlePageChange(page: number) {
  currentPage.value = page
}

function handleSizeChange(size: number) {
  pageSize.value = size
  currentPage.value = 1
}

watch(
  () => uiStore.selectedFolderId,
  () => {
    if (props.scope === 'mine') {
      currentPage.value = 1
    }
  },
)

watch(
  [() => uiStore.folderTreeReady, folderTree, selectedFolderId],
  ([ready, folders, folderId]) => {
    if (props.scope !== 'mine' || !ready || folderId == null || !folders) {
      return
    }

    if (!findFolderById(folders, folderId)) {
      uiStore.clearSelectedFolderId()
      currentPage.value = 1
    }
  },
  { immediate: true },
)

function formatDate(value: string) {
  return new Date(value).toLocaleString('zh-TW')
}

function findFolderName(nodes: FolderTreeNode[], targetId: number): string | null {
  for (const node of nodes) {
    if (node.id === targetId) return node.name
    if (node.children?.length) {
      const match = findFolderName(node.children, targetId)
      if (match) return match
    }
  }
  return null
}

function findFolderById(nodes: FolderTreeNode[], targetId: number): boolean {
  for (const node of nodes) {
    if (node.id === targetId) {
      return true
    }
    if (node.children?.length && findFolderById(node.children, targetId)) {
      return true
    }
  }
  return false
}
</script>

<style scoped>
.document-panel {
  min-height: 100%;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 24px 24px 18px;
  border-bottom: 1px solid #edf2f7;
}

.section-header h3,
.section-header p {
  margin: 0;
}

.section-header p {
  margin-top: 8px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.header-icon {
  border: 1px solid #d9e1ea;
  background: #fff;
  color: #526071;
  padding: 10px 12px;
  cursor: pointer;
}

.table-container {
  padding: 20px 24px 24px;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  padding: 0 24px 24px;
}

@media (max-width: 768px) {
  .section-header {
    flex-direction: column;
    align-items: stretch;
    padding: 16px;
  }

  .header-actions {
    flex-wrap: wrap;
  }

  .table-container {
    padding: 16px;
  }

  .pagination {
    justify-content: center;
    padding: 0 16px 16px;
  }
}
</style>
