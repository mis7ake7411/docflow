<template>
  <div class="page-card card-section">
    <div class="section-header">
      <h3>最近瀏覽</h3>
      <span class="muted">最近查閱的文件</span>
    </div>

    <el-skeleton v-if="isLoading" :rows="5" animated />
    <el-alert v-else-if="error" title="最近瀏覽載入失敗" type="error" show-icon :closable="false" />
    <el-empty v-else-if="!items.length" description="目前沒有最近瀏覽紀錄" />

    <div v-else class="table-wrapper">
      <el-table :data="items" stripe>
        <el-table-column prop="title" label="標題" min-width="180" />
        <el-table-column label="狀態" width="120">
          <template #default="scope">
            {{ getStatusLabel(scope.row.status) }}
          </template>
        </el-table-column>
        <el-table-column label="瀏覽分數" width="140">
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
import { getStatusLabel } from '@/shared/utils/display'

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

