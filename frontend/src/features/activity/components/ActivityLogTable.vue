<template>
  <div class="page-card card-section">
    <div class="section-header">
      <h3>Activity Logs</h3>
      <span class="muted">最近系統活動</span>
    </div>

    <el-skeleton v-if="isLoading" :rows="6" animated />
    <el-alert v-else-if="error" title="活動紀錄載入失敗" type="error" show-icon :closable="false" />
    <el-empty v-else-if="!items.length" description="目前沒有活動紀錄" />

    <div v-else class="table-wrapper">
      <el-table :data="items" stripe>
      <el-table-column prop="action" label="Action" width="140" />
      <el-table-column prop="targetType" label="Target Type" width="140" />
      <el-table-column prop="targetId" label="Target ID" width="120" />
      <el-table-column label="Detail" min-width="260">
        <template #default="scope">
          <span class="detail-text">{{ scope.row.detailJson || '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="Created At" min-width="180">
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

const { data, isLoading, error } = useQuery({
  queryKey: ['activities', 'recent'],
  queryFn: getActivities,
})

const items = computed(() => data.value ?? [])

function formatDate(value: string) {
  return new Date(value).toLocaleString()
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
