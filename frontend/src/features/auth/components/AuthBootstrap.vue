<template>
  <slot v-if="authStore.initialized" />
  <div v-else class="auth-bootstrap">
    <div class="page-card bootstrap-card">
      <h1>DocFlow Lite</h1>
      <p class="muted">正在驗證登入狀態...</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { registerAuthBridge } from '@/shared/api/auth-bridge'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const router = useRouter()

registerAuthBridge({
  getAccessToken: () => authStore.accessToken,
  refreshAccessToken: () => authStore.refreshAccessToken(),
  onLogout: () => {
    authStore.clearAuth()
    authStore.initialized = true
  },
})

onMounted(async () => {
  await authStore.bootstrapAuth()
  if (!authStore.isAuthenticated && router.currentRoute.value.path !== '/login') {
    await router.replace('/login')
  }
})
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
