<template>
  <AppLayout>
    <div class="insights-grid">
      <HotDocumentsCard />
      <RecentViewsCard />
      <div class="full-width">
        <ActivityLogTable />
      </div>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import AppLayout from '@/layouts/AppLayout.vue'
import HotDocumentsCard from '@/features/stats/components/HotDocumentsCard.vue'
import RecentViewsCard from '@/features/stats/components/RecentViewsCard.vue'
import ActivityLogTable from '@/features/activity/components/ActivityLogTable.vue'
</script>

<style scoped>
.insights-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 24px;
}

.full-width {
  grid-column: 1 / -1;
}
</style>
