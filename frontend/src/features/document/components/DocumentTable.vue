<template>
  <div>
    <div class="section-header">
      <div>
        <h3>Documents</h3>
        <p class="muted">{{ sectionDescription }}</p>
      </div>
    </div>

    <el-skeleton v-if="isLoading" :rows="8" animated />
    <el-alert v-else-if="error" title="Document list 載入失敗" type="error" show-icon :closable="false" />
    <el-empty v-else-if="!filteredDocuments.length" description="目前沒有文件" />

    <el-table v-else :data="filteredDocuments" stripe>
      <el-table-column prop="title" label="Title" min-width="220" />
      <el-table-column prop="status" label="Status" width="120" />
      <el-table-column prop="version" label="Version" width="100" />
      <el-table-column prop="fileName" label="File" min-width="180" />
      <el-table-column label="Updated At" min-width="180">
        <template #default="scope">
          {{ formatDate(scope.row.updatedAt) }}
        </template>
      </el-table-column>
      <el-table-column label="Actions" width="140" fixed="right">
        <template #default="scope">
          <el-button text type="primary" @click="openDetail(scope.row.id)">查看</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useQuery } from '@tanstack/vue-query'
import { getDocuments } from '@/features/document/api'
import { useUiStore } from '@/stores/ui'

const router = useRouter()
const uiStore = useUiStore()

const { data, isLoading, error } = useQuery({
  queryKey: ['documents', 'list'],
  queryFn: getDocuments,
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

function formatDate(value: string) {
  return new Date(value).toLocaleString()
}
</script>

<style scoped>
.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
</style>
