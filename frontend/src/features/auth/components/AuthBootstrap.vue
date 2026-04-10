<template>
  <slot v-if="authStore.initialized" />
  <div v-else class="auth-bootstrap">
    <div class="page-card bootstrap-card">
      <h1>DocFlow Lite</h1>
      <p class="muted">正在驗證登入狀態，請稍候...</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
</script>

<style scoped>
.auth-bootstrap {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.bootstrap-card {
  width: 100%;
  max-width: 420px;
}
</style>
