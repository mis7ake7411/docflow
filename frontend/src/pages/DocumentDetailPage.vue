<template>
  <AppLayout>
    <div class="document-detail-page">
      <div class="page-card">
        <div class="header-row">
          <div>
            <h1>Document Detail</h1>
            <p class="muted">查看、更新、上傳與下載文件。</p>
          </div>
          <div class="action-group" v-if="document">
            <el-button @click="openEditDialog">編輯</el-button>
            <el-button type="primary" @click="uploadDialogVisible = true">上傳檔案</el-button>
            <el-button type="success" :disabled="!document.storedFileName" @click="handleDownload">下載</el-button>
          </div>
        </div>

        <el-skeleton v-if="isLoading" :rows="8" animated />
        <el-alert v-else-if="error" title="Document detail 載入失敗" type="error" show-icon :closable="false" />
        <DocumentDetailCard v-else-if="document" :document="document" />
        <el-empty v-else description="找不到文件" />
      </div>
    </div>

    <DocumentFormDialog v-model="editDialogVisible" :document="document ?? null" />
    <DocumentUploadDialog v-model="uploadDialogVisible" :document-id="documentId" />
  </AppLayout>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useQuery } from '@tanstack/vue-query'
import { ElMessage } from 'element-plus'
import AppLayout from '@/layouts/AppLayout.vue'
import { downloadDocumentFile, getDocumentDetail } from '@/features/document/api'
import DocumentDetailCard from '@/features/document/components/DocumentDetailCard.vue'
import DocumentFormDialog from '@/features/document/components/DocumentFormDialog.vue'
import DocumentUploadDialog from '@/features/document/components/DocumentUploadDialog.vue'

const route = useRoute()
const editDialogVisible = ref(false)
const uploadDialogVisible = ref(false)

const documentId = computed(() => Number(route.params.id))

const { data, isLoading, error } = useQuery({
  queryKey: ['documents', 'detail', documentId],
  queryFn: () => getDocumentDetail(documentId.value),
})

const document = computed(() => data.value ?? null)

function openEditDialog() {
  editDialogVisible.value = true
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
  ElMessage.success('下載已開始')
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
}

.action-group {
  display: flex;
  gap: 12px;
}
</style>
