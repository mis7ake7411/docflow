<template>
  <div>
    <div class="section-header">
      <div>
        <h3>文件</h3>
        <p class="muted">{{ sectionDescription }}</p>
      </div>
      <el-button type="primary" @click="openCreateDialog">新增文件</el-button>
    </div>

    <el-skeleton v-if="isLoading" :rows="8" animated />
    <el-alert v-else-if="error" title="文件清單載入失敗" type="error" show-icon :closable="false" />
    <el-empty v-else-if="!filteredDocuments.length" description="目前沒有文件" />

    <div v-else class="table-wrapper">
      <el-table :data="filteredDocuments" stripe>
        <el-table-column prop="title" label="標題" min-width="220" />
        <el-table-column label="狀態" width="120">
          <template #default="scope">
            {{ getStatusLabel(scope.row.status) }}
          </template>
        </el-table-column>
        <el-table-column prop="version" label="版本" width="100" />
        <el-table-column prop="fileName" label="檔名" min-width="180" />
        <el-table-column label="更新時間" min-width="180">
          <template #default="scope">
            {{ formatDate(scope.row.updatedAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220">
          <template #default="scope">
            <el-button text type="primary" @click="openDetail(scope.row.id)">查看</el-button>
            <el-button text @click="openEditDialog(scope.row)">編輯</el-button>
            <el-popconfirm title="確定刪除這份文件？" @confirm="handleDelete(scope.row.id)">
              <template #reference>
                <el-button text type="danger">刪除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <DocumentFormDialog v-model="createDialogVisible" :document="null" />
    <DocumentFormDialog v-model="editDialogVisible" :document="editingDocument" />
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useMutation, useQuery, useQueryClient } from '@tanstack/vue-query'
import { ElMessage } from 'element-plus'
import { deleteDocument, getDocuments, type DocumentItem } from '@/features/document/api'
import DocumentFormDialog from '@/features/document/components/DocumentFormDialog.vue'
import { useUiStore } from '@/stores/ui'
import { getStatusLabel } from '@/shared/utils/display'

const router = useRouter()
const uiStore = useUiStore()
const queryClient = useQueryClient()

const createDialogVisible = ref(false)
const editDialogVisible = ref(false)
const editingDocument = ref<DocumentItem | null>(null)

const { data, isLoading, error } = useQuery({
  queryKey: ['documents', 'list'],
  queryFn: getDocuments,
})

const deleteMutation = useMutation({
  mutationFn: deleteDocument,
  onSuccess: async () => {
    await queryClient.invalidateQueries({ queryKey: ['documents', 'list'] })
    ElMessage.success('文件已刪除')
  },
})

const filteredDocuments = computed(() => {
  const documents = data.value ?? []
  if (!uiStore.selectedFolderId) {
    return documents
  }
  return documents.filter((document) => document.folderId === uiStore.selectedFolderId)
})

const sectionDescription = computed(() => {
  if (!uiStore.selectedFolderId) {
    return '顯示全部文件'
  }
  return `目前顯示資料夾 #${uiStore.selectedFolderId} 的文件`
})

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

async function handleDelete(documentId: number) {
  await deleteMutation.mutateAsync(documentId)
}

function formatDate(value: string) {
  return new Date(value).toLocaleString('zh-TW')
}
</script>

<style scoped>
.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.table-wrapper {
  overflow-x: auto;
}

@media (max-width: 768px) {
  .section-header {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
  }
}
</style>
