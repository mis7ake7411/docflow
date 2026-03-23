<template>
  <div class="page-card card-section">
    <div class="section-header">
      <h3>熱門文件</h3>
      <span class="muted">依熱度分數排序</span>
    </div>

    <el-skeleton v-if="isLoading" :rows="5" animated />
    <el-alert v-else-if="error" title="熱門文件載入失敗" type="error" show-icon :closable="false" />
    <el-empty v-else-if="!items.length" description="目前沒有熱門文件資料" />

    <div v-else class="table-wrapper">
      <el-table :data="items" stripe>
        <el-table-column prop="title" label="標題" min-width="180" />
        <el-table-column label="狀態" width="120">
          <template #default="scope">
            {{ getStatusLabel(scope.row.status) }}
          </template>
        </el-table-column>
        <el-table-column prop="score" label="分數" width="120" />
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useQuery } from '@tanstack/vue-query'
import { getHotDocuments } from '@/features/stats/api'
import { getStatusLabel } from '@/shared/utils/display'

const { data, isLoading, error } = useQuery({
  queryKey: ['stats', 'hot-documents'],
  queryFn: getHotDocuments,
})

const items = computed(() => data.value ?? [])
</script>

<style scoped>
.card-section {
  min-height: 260px;
}

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
    gap: 8px;
  }
}
</style>

