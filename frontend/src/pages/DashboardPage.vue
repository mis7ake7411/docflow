<template>
  <AppLayout>
    <div class="dashboard-grid">
      <TotalDocumentsCard />
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
import TotalDocumentsCard from '@/features/stats/components/TotalDocumentsCard.vue'
import ActivityLogTable from '@/features/activity/components/ActivityLogTable.vue'
</script>

<style scoped>
.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 24px;
}

.full-width {
  grid-column: 1 / -1;
}

@media (max-width: 1100px) {
  .dashboard-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .full-width {
    grid-column: 1 / -1;
  }
}

@media (max-width: 768px) {
  .dashboard-grid {
    grid-template-columns: 1fr;
    gap: 16px;
  }
}
</style>

