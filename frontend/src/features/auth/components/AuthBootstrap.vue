<template>
  <slot />
</template>

<script setup lang="ts">
import { registerAuthBridge } from '@/shared/api/auth-bridge'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()

registerAuthBridge({
  getAccessToken: () => authStore.accessToken,
  refreshAccessToken: () => authStore.refreshAccessToken(),
  onLogout: () => {
    authStore.clearAuth()
  },
})
</script>
