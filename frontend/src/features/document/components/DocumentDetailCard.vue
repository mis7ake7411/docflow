<template>
  <div class="detail-grid" v-if="document">
    <div class="detail-item"><strong>Title:</strong> {{ document.title }}</div>
    <div class="detail-item"><strong>Status:</strong> {{ document.status }}</div>
    <div class="detail-item"><strong>Version:</strong> {{ document.version }}</div>
    <div class="detail-item"><strong>Folder ID:</strong> {{ document.folderId ?? 'None' }}</div>
    <div class="detail-item full"><strong>Description:</strong> {{ document.description || '—' }}</div>
    <div class="detail-item"><strong>File Name:</strong> {{ document.fileName || '—' }}</div>
    <div class="detail-item"><strong>Content Type:</strong> {{ document.contentType || '—' }}</div>
    <div class="detail-item"><strong>File Size:</strong> {{ formatFileSize(document.fileSize) }}</div>
    <div class="detail-item"><strong>Updated At:</strong> {{ formatDate(document.updatedAt) }}</div>
  </div>
</template>

<script setup lang="ts">
import type { DocumentItem } from '@/features/document/api'

defineProps<{
  document: DocumentItem | null
}>()

function formatDate(value: string) {
  return new Date(value).toLocaleString()
}

function formatFileSize(size: number | null) {
  if (!size) {
    return '—'
  }
  if (size < 1024) {
    return `${size} B`
  }
  if (size < 1024 * 1024) {
    return `${(size / 1024).toFixed(1)} KB`
  }
  return `${(size / (1024 * 1024)).toFixed(1)} MB`
}
</script>

<style scoped>
.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.detail-item {
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: #fff;
}

.full {
  grid-column: 1 / -1;
}
</style>
