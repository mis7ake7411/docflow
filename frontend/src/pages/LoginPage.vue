<template>
  <div class="page-shell login-page">
    <div class="page-card login-card">
      <h1>DocFlow Lite</h1>
      <p class="muted">登入後即可管理文件、資料夾與活動紀錄。</p>
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
