<template>
  <div class="page-card card-section">
    <div class="section-header">
      <h3>Recent Views</h3>
      <span class="muted">最近查看文件</span>
    </div>

    <el-skeleton v-if="isLoading" :rows="5" animated />
    <el-alert v-else-if="error" title="最近查看資料載入失敗" type="error" show-icon :closable="false" />
    <el-empty v-else-if="!items.length" description="目前沒有最近查看資料" />

    <div v-else class="table-wrapper">
      <el-table :data="items" stripe>
      <el-table-column prop="title" label="Title" min-width="180" />
      <el-table-column prop="status" label="Status" width="120" />
      <el-table-column label="Viewed Score" width="140">
        <template #default="scope">
          {{ scope.row.score }}
        </template>
      </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useQuery } from '@tanstack/vue-query'
import { getRecentViews } from '@/features/stats/api'

const { data, isLoading, error } = useQuery({
  queryKey: ['users', 'me', 'recent-views'],
  queryFn: getRecentViews,
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
