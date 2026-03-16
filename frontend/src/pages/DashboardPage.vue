<template>
  <AppLayout>
    <div class="dashboard-grid">
      <div class="page-card">
        <FolderTree />
      </div>

      <div class="page-card">
        <DocumentTable />
      </div>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import AppLayout from '@/layouts/AppLayout.vue'
import FolderTree from '@/features/folder/components/FolderTree.vue'
import DocumentTable from '@/features/document/components/DocumentTable.vue'
</script>

<style scoped>
.dashboard-grid {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 24px;
}
</style>
