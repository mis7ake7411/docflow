<template>
  <AppLayout>
    <div class="shared-documents-page">
      <section class="page-card">

        <DocumentTable
          scope="shared"
          :show-create-button="false"
          header-title="分享文件"
          header-description="列出分享給你的文件"
        />
      </section>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import AppLayout from '@/layouts/AppLayout.vue'
import DocumentTable from '@/features/document/components/DocumentTable.vue'
</script>

<style scoped>
.shared-documents-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.page-card {
  background: #ffffff;
  border: 1px solid #e5ebf1;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.04);
}

.header-row h1 {
  margin: 0;
  font-size: 1.8rem;
}

.header-row p {
  margin: 8px 0 0;
}

@media (max-width: 768px) {

  .header-row h1 {
    font-size: 1.5rem;
  }
}
</style>
