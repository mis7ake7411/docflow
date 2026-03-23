<template>
  <div class="page-card card-section">
    <div class="section-header">
      <h3>總文件數</h3>
      <span class="muted">所有文件統計</span>
    </div>

    <el-skeleton v-if="isLoading" :rows="2" animated />
    <el-alert v-else-if="error" title="總文件數載入失敗" type="error" show-icon :closable="false" />
    <div v-else class="total-value">
      <span class="value">{{ totalLabel }}</span>
      <span class="unit">筆</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useQuery } from '@tanstack/vue-query'
import { getDocuments } from '@/features/document/api'

const { data, isLoading, error } = useQuery({
  queryKey: ['documents', 'total'],
  queryFn: () => getDocuments(0, 1),
})

const totalLabel = computed(() => {
  const total = data.value?.totalElements ?? 0
  return new Intl.NumberFormat('zh-TW').format(total)
})
</script>

<style scoped>
.card-section {
  min-height: 220px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.total-value {
  display: flex;
  align-items: baseline;
  gap: 12px;
  padding: 12px 0 6px;
}

.value {
  font-size: 2.6rem;
  font-weight: 700;
  color: #0f172a;
}

.unit {
  color: #64748b;
  font-size: 1rem;
}

@media (max-width: 768px) {
  .section-header {
    flex-direction: column;
    align-items: stretch;
    gap: 6px;
  }
}
</style>
