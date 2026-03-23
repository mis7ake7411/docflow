<template>
  <div class="document-panel">
    <div class="section-header">
      <div>
        <h3>文件列表</h3>
        <p class="muted">{{ sectionDescription }}</p>
      </div>

      <div class="header-actions">
        <button type="button" class="header-icon" @click="refreshTable">刷新</button>
        <button type="button" class="header-icon">設定</button>
        <el-button type="primary" @click="openCreateDialog">新增文件</el-button>
      </div>
    </div>

    <div class="table-container">
      <el-skeleton v-if="isLoading" :rows="8" animated />
      <el-alert v-else-if="error" title="文件清單載入失敗" type="error" show-icon :closable="false" />
      <el-empty v-else-if="!items.length" description="目前沒有文件" />

      <el-table v-else :data="items" stripe>
        <el-table-column prop="title" label="標題" min-width="220" />
        <el-table-column label="狀態" width="120">
          <template #default="scope">
            {{ getStatusLabel(scope.row.status) }}
          </template>
        </el-table-column>
        <el-table-column prop="version" label="版本" width="90" />
        <el-table-column prop="fileName" label="檔名" min-width="160" />
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
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
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
const currentPage = ref(1)
const pageSize = ref(10)
const pageSizes = [10, 20, 50]

const { data, isLoading, error, refetch } = useQuery({
  queryKey: computed(() => ['documents', 'list', currentPage.value, pageSize.value, uiStore.selectedFolderId]),
  queryFn: () => getDocuments(currentPage.value - 1, pageSize.value, uiStore.selectedFolderId),
})

const deleteMutation = useMutation({
  mutationFn: deleteDocument,
  onSuccess: async () => {
    await queryClient.invalidateQueries({ queryKey: ['documents', 'list'] })
    ElMessage.success('文件已刪除')
  },
})

const items = computed(() => data.value?.items ?? [])
const totalElements = computed(() => data.value?.totalElements ?? 0)

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

function refreshTable() {
  void refetch()
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
    currentPage.value = 1
  },
)

function formatDate(value: string) {
  return new Date(value).toLocaleString('zh-TW')
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
