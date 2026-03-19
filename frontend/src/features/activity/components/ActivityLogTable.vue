<template>
  <div class="page-card card-section">
    <div class="section-header">
      <h3>活動紀錄</h3>
      <span class="muted">最近系統活動</span>
    </div>

    <el-skeleton v-if="isLoading" :rows="6" animated />
    <el-alert v-else-if="error" title="活動紀錄載入失敗" type="error" show-icon :closable="false" />
    <el-empty v-else-if="!items.length" description="目前沒有活動紀錄" />

    <div v-else class="table-wrapper">
      <el-table :data="items" stripe>
        <el-table-column label="動作" width="140">
          <template #default="scope">
            {{ getActionLabel(scope.row.action) }}
          </template>
        </el-table-column>
        <el-table-column label="目標類型" width="140">
          <template #default="scope">
            {{ getTargetTypeLabel(scope.row.targetType) }}
          </template>
        </el-table-column>
        <el-table-column prop="targetId" label="目標編號" width="120" />
        <el-table-column label="內容" min-width="260">
          <template #default="scope">
            <span class="detail-text">{{ scope.row.detailJson || '無' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="建立時間" min-width="180">
          <template #default="scope">
            {{ formatDate(scope.row.createdAt) }}
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useQuery } from '@tanstack/vue-query'
import { getActivities } from '@/features/activity/api'
import { getActionLabel, getTargetTypeLabel } from '@/shared/utils/display'

const { data, isLoading, error } = useQuery({
  queryKey: ['activities', 'recent'],
  queryFn: getActivities,
})

const items = computed(() => data.value ?? [])

function formatDate(value: string) {
  return new Date(value).toLocaleString('zh-TW')
}
</script>

<style scoped>
.card-section {
  min-height: 320px;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.detail-text {
  word-break: break-word;
}

.table-wrapper {
  overflow-x: auto;
}

@media (max-width: 768px) {
  .section-header {
    flex-direction: column;
    align-items: stretch;
    gap: 8px;
  }
}
</style>
