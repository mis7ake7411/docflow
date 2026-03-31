<template>
  <div v-if="document" class="detail-grid">
    <div class="detail-item"><strong>標題：</strong>{{ document.title }}</div>
    <div class="detail-item"><strong>狀態：</strong>{{ getStatusLabel(document.status) }}</div>
    <div class="detail-item"><strong>版本：</strong>{{ document.version }}</div>
    <div class="detail-item"><strong>資料夾編號：</strong>{{ document.folderId ?? '根目錄' }}</div>
    <div class="detail-item"><strong>建立者：</strong>{{ document.createdBy }}</div>
    <div v-if="document.accessLevel" class="detail-item">
      <strong>存取權限：</strong>{{ getAccessLevelLabel(document.accessLevel) }}
    </div>
    <div v-if="document.sharedBy" class="detail-item">
      <strong>分享者：</strong>{{ document.sharedBy }}
    </div>
    <div class="detail-item full"><strong>描述：</strong>{{ document.description || '未填寫' }}</div>
    <div class="detail-item"><strong>檔名：</strong>{{ document.fileName || '尚未上傳' }}</div>
    <div class="detail-item"><strong>檔案類型：</strong>{{ document.contentType || '未提供' }}</div>
    <div class="detail-item"><strong>檔案大小：</strong>{{ formatFileSize(document.fileSize) }}</div>
    <div class="detail-item"><strong>最後更新：</strong>{{ formatDate(document.updatedAt) }}</div>
  </div>
</template>

<script setup lang="ts">
import type { DocumentItem } from '@/features/document/api'
import { getAccessLevelLabel, getStatusLabel } from '@/shared/utils/display'

defineProps<{
  document: DocumentItem | null
}>()

function formatDate(value: string) {
  return new Date(value).toLocaleString('zh-TW')
}

function formatFileSize(size: number | null) {
  if (!size) return '未提供'
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
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

@media (max-width: 768px) {
  .detail-grid {
    grid-template-columns: 1fr;
  }

  .full {
    grid-column: auto;
  }
}
</style>
