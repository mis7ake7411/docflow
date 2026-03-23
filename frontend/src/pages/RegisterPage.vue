<template>
  <div class="page-shell register-page">
    <div class="page-card register-card">
      <h1>建立帳號</h1>
      <p class="muted">建立帳號後即可開始使用文件管理。</p>
      <RegisterForm />
      <RouterLink class="back-link" to="/login">返回登入</RouterLink>
    </div>
  </div>
</template>

<script setup lang="ts">
import RegisterForm from '@/features/auth/components/RegisterForm.vue'
</script>

<style scoped>
.register-page {
  display: flex;
  align-items: center;
  justify-content: center;
}

.register-card {
  width: 100%;
  max-width: 440px;
}

.back-link {
  display: inline-block;
  margin-top: 16px;
  color: #2563eb;
  text-decoration: none;
  font-weight: 600;
}

.back-link:hover {
  text-decoration: underline;
}
</style>
