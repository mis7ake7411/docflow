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
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useQuery } from '@tanstack/vue-query'
import { getActivities } from '@/features/activity/api'
import { getActionLabel, getTargetTypeLabel } from '@/shared/utils/display'

const currentPage = ref(1)
const pageSize = ref(10)
const pageSizes = [10, 20, 50]

const { data, isLoading, error } = useQuery({
  queryKey: computed(() => ['activities', 'recent', currentPage.value, pageSize.value]),
  queryFn: () => getActivities(currentPage.value - 1, pageSize.value),
})

const items = computed(() => data.value?.items ?? [])
const totalElements = computed(() => data.value?.totalElements ?? 0)

function formatDate(value: string) {
  return new Date(value).toLocaleString('zh-TW')
}

function handlePageChange(page: number) {
  currentPage.value = page
}

function handleSizeChange(size: number) {
  pageSize.value = size
  currentPage.value = 1
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

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

@media (max-width: 768px) {
  .section-header {
    flex-direction: column;
    align-items: stretch;
    gap: 8px;
  }

  .pagination {
    justify-content: center;
  }
}
</style>
