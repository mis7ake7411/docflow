<template>
  <AppLayout>
    <div class="user-management-page">
      <section class="page-card">
        <div class="header-row">
          <div>
            <h1>使用者管理</h1>
            <p class="muted">新增帳號、調整角色與狀態。</p>
          </div>
        </div>

        <UserTable />
      </section>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import AppLayout from '@/layouts/AppLayout.vue'
import UserTable from '@/features/user/components/UserTable.vue'
</script>

<style scoped>
.user-management-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.page-card {
  background: #ffffff;
  border: 1px solid #e5ebf1;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.04);
}

.header-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: 24px 28px 12px;
  border-bottom: 1px solid #e9eef4;
}

.header-row h1 {
  margin: 0;
  font-size: 1.8rem;
}

.header-row p {
  margin: 8px 0 0;
}

@media (max-width: 768px) {
  .header-row {
    flex-direction: column;
    padding: 18px 16px 12px;
  }

  .header-row h1 {
    font-size: 1.5rem;
  }
}
</style>
