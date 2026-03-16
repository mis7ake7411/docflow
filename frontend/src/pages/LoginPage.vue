<template>
  <div class="page-shell login-page">
    <div class="page-card login-card">
      <h1>DocFlow Lite</h1>
      <p class="muted">請登入以操作文件管理系統。</p>
      <LoginForm />
    </div>
  </div>
</template>

<script setup lang="ts">
import LoginForm from '@/features/auth/components/LoginForm.vue'
</script>

<style scoped>
.login-page {
  display: flex;
  align-items: center;
  justify-content: center;
}

.login-card {
  width: 100%;
  max-width: 420px;
}
</style>
