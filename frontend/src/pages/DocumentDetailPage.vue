<template>
  <AppLayout>
    <div class="document-detail-page">
      <div class="page-card">
        <div class="header-row">
          <div>
            <h1>文件明細</h1>
            <p class="muted">檢視文件內容、編輯欄位與下載附件。</p>
          </div>
          <div v-if="document" class="action-group">
            <template v-if="!isManager">
              <el-tooltip v-if="!canEditDocument(document)" :content="PERMISSION_MESSAGES.documentHint">
                <span>
                  <el-button disabled>編輯</el-button>
                </span>
              </el-tooltip>
              <el-button v-else @click="openEditDialog">編輯</el-button>

              <el-tooltip v-if="!canEditDocument(document)" :content="PERMISSION_MESSAGES.documentHint">
                <span>
                  <el-button type="primary" disabled>上傳檔案</el-button>
                </span>
              </el-tooltip>
              <el-button v-else type="primary" @click="openUploadDialog">上傳檔案</el-button>

              <el-tooltip v-if="!canEditDocument(document)" :content="PERMISSION_MESSAGES.documentHint">
                <span>
                  <el-button type="danger" disabled>刪除</el-button>
                </span>
              </el-tooltip>
              <el-popconfirm v-else title="確定刪除這份文件？" @confirm="handleDelete">
                <template #reference>
                  <el-button type="danger">刪除</el-button>
                </template>
              </el-popconfirm>
            </template>
            <el-button type="success" :disabled="!document.storedFileName" @click="handleDownload">下載檔案</el-button>
          </div>
        </div>
        <p v-if="document && !canEditDocument(document)" class="muted">你僅能修改自己建立的文件</p>

        <el-skeleton v-if="isLoading" :rows="8" animated />
        <el-alert v-else-if="error" title="文件明細載入失敗" type="error" show-icon :closable="false" />
        <DocumentDetailCard v-else-if="document" :document="document" />
        <el-empty v-else description="找不到文件資料" />
      </div>
    </div>

    <DocumentFormDialog v-model="editDialogVisible" :document="document ?? null" />
    <DocumentUploadDialog v-model="uploadDialogVisible" :document-id="documentId" />
  </AppLayout>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useMutation, useQuery, useQueryClient } from '@tanstack/vue-query'
import { ElMessage } from 'element-plus'
import AppLayout from '@/layouts/AppLayout.vue'
import { deleteDocument, downloadDocumentFile, getDocumentDetail } from '@/features/document/api'
import DocumentDetailCard from '@/features/document/components/DocumentDetailCard.vue'
import DocumentFormDialog from '@/features/document/components/DocumentFormDialog.vue'
import DocumentUploadDialog from '@/features/document/components/DocumentUploadDialog.vue'
import { useAuthStore } from '@/stores/auth'
import { canModifyResource, PERMISSION_MESSAGES } from '@/shared/utils/permission'
import { isAxiosError } from 'axios'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const queryClient = useQueryClient()
const editDialogVisible = ref(false)
const uploadDialogVisible = ref(false)

const documentId = computed(() => Number(route.params.id))

const { data, isLoading, error } = useQuery({
  queryKey: ['documents', 'detail', documentId],
  queryFn: () => getDocumentDetail(documentId.value),
})

const document = computed(() => data.value ?? null)
const isManager = computed(() => authStore.userRole === 'MANAGER')
const currentUser = computed(() => authStore.user)

const deleteMutation = useMutation({
  mutationFn: (id: number) => deleteDocument(id),
  onSuccess: async () => {
    await queryClient.invalidateQueries({ queryKey: ['documents', 'list'] })
    ElMessage.success('文件已刪除')
    await router.push('/app/documents')
  },
  onError: (error) => {
    if (isAxiosError(error) && error.response?.status === 403) {
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

function canEditDocument(value: { createdBy: number | null | undefined }) {
  return canModifyResource(value.createdBy, currentUser.value)
}

async function handleDelete() {
  if (!document.value) {
    return
  }
  await deleteMutation.mutateAsync(document.value.id)
}

async function handleDownload() {
  if (!document.value) {
    return
  }

  const blob = await downloadDocumentFile(document.value.id)
  const url = window.URL.createObjectURL(blob)
  const link = window.document.createElement('a')
  link.href = url
  link.download = document.value.fileName || `document-${document.value.id}`
  link.click()
  window.URL.revokeObjectURL(url)
  ElMessage.success('檔案下載成功')
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
