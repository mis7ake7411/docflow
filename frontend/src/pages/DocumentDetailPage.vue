<template>
  <AppLayout>
    <div class="document-detail-page">
      <div class="page-card">
        <div class="header-row">
          <div>
            <h1>文件詳情</h1>
            <p class="muted">查看文件內容、編輯欄位、上傳檔案與管理分享設定。</p>
          </div>
          <div v-if="document" class="action-group">
            <el-button v-if="canShare" type="warning" plain @click="shareDialogVisible = true">分享</el-button>

            <template v-if="canEdit">
              <el-button @click="openEditDialog">編輯</el-button>
              <el-button type="primary" @click="openUploadDialog">上傳檔案</el-button>
            </template>
            <template v-else>
              <el-tooltip :content="accessHint">
                <span><el-button disabled>編輯</el-button></span>
              </el-tooltip>
              <el-tooltip :content="accessHint">
                <span><el-button type="primary" disabled>上傳檔案</el-button></span>
              </el-tooltip>
            </template>

            <template v-if="canDelete">
              <el-popconfirm title="確定刪除這份文件？" @confirm="handleDelete">
                <template #reference>
                  <el-button type="danger">刪除</el-button>
                </template>
              </el-popconfirm>
            </template>
            <template v-else>
              <el-tooltip :content="PERMISSION_MESSAGES.documentForbidden">
                <span><el-button type="danger" disabled>刪除</el-button></span>
              </el-tooltip>
            </template>

            <el-button type="success" :disabled="!document.storedFileName" @click="handleDownload">下載檔案</el-button>
          </div>
        </div>

        <p v-if="document && !canEdit" class="muted">{{ accessHint }}</p>

        <el-skeleton v-if="isLoading" :rows="8" animated />
        <el-alert v-else-if="error" title="文件詳情載入失敗" type="error" show-icon :closable="false" />
        <DocumentDetailCard v-else-if="document" :document="document" />
        <el-empty v-else description="找不到文件資料" />
      </div>
    </div>

    <DocumentFormDialog v-model="editDialogVisible" :document="document ?? null" />
    <DocumentUploadDialog v-model="uploadDialogVisible" :document-id="documentId" />
    <DocumentShareDialog
      v-model="shareDialogVisible"
      :document-id="documentId"
      :document-title="document?.title ?? ''"
    />
  </AppLayout>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useMutation, useQuery, useQueryClient } from '@tanstack/vue-query'
import { ElMessage } from 'element-plus'
import { isAxiosError } from 'axios'
import AppLayout from '@/layouts/AppLayout.vue'
import { deleteDocument, downloadDocumentFile, getDocumentDetail } from '@/features/document/api'
import DocumentDetailCard from '@/features/document/components/DocumentDetailCard.vue'
import DocumentFormDialog from '@/features/document/components/DocumentFormDialog.vue'
import DocumentShareDialog from '@/features/document/components/DocumentShareDialog.vue'
import DocumentUploadDialog from '@/features/document/components/DocumentUploadDialog.vue'
import {
  canDeleteDocument,
  canEditDocument,
  canShareDocument,
  getDocumentAccessHint,
  PERMISSION_MESSAGES,
} from '@/shared/utils/permission'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const queryClient = useQueryClient()
const editDialogVisible = ref(false)
const uploadDialogVisible = ref(false)
const shareDialogVisible = ref(false)

const documentId = computed(() => Number(route.params.id))

const { data, isLoading, error } = useQuery({
  queryKey: ['documents', 'detail', documentId],
  queryFn: () => getDocumentDetail(documentId.value),
})

const document = computed(() => data.value ?? null)
const currentUser = computed(() => authStore.user)
const canEdit = computed(() => document.value ? canEditDocument(document.value, currentUser.value) : false)
const canDelete = computed(() => document.value ? canDeleteDocument(document.value, currentUser.value) : false)
const canShare = computed(() => document.value ? canShareDocument(document.value, currentUser.value) : false)
const accessHint = computed(() =>
  document.value ? getDocumentAccessHint(document.value, currentUser.value) : PERMISSION_MESSAGES.documentForbidden,
)

const deleteMutation = useMutation({
  mutationFn: (id: number) => deleteDocument(id),
  onSuccess: async () => {
    await queryClient.invalidateQueries({ queryKey: ['documents'] })
    ElMessage.success('文件已刪除')
    await router.push('/app/files')
  },
  onError: (mutationError) => {
    if (isAxiosError(mutationError) && mutationError.response?.status === 403) {
      ElMessage.error(PERMISSION_MESSAGES.documentForbidden)
    }
  },
})

function openEditDialog() {
  editDialogVisible.value = true
}

function openUploadDialog() {
  uploadDialogVisible.value = true
}

async function handleDelete() {
  if (!document.value) return
  await deleteMutation.mutateAsync(document.value.id)
}

async function handleDownload() {
  if (!document.value) return

  const blob = await downloadDocumentFile(document.value.id)
  const url = window.URL.createObjectURL(blob)
  const link = window.document.createElement('a')
  link.href = url
  link.download = document.value.fileName || `document-${document.value.id}`
  link.click()
  window.URL.revokeObjectURL(url)
  ElMessage.success('檔案下載完成')
}
</script>

<style scoped>
.document-detail-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.header-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 20px;
  gap: 16px;
}

.action-group {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

@media (max-width: 768px) {
  .header-row {
    flex-direction: column;
    margin-bottom: 16px;
  }

  .action-group {
    width: 100%;
    justify-content: stretch;
  }

  .action-group :deep(.el-button) {
    flex: 1 1 140px;
    margin-left: 0;
  }
}
</style>
