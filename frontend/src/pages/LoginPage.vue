<template>
  <div class="page-shell login-page">
    <div class="page-card login-card">
      <h1>DocFlow Lite</h1>
      <p class="muted">使用你的帳號密碼登入，開始管理文件與分享權限。</p>

      <el-alert
        v-if="sessionExpiredReason"
        :title="sessionExpiredReason"
        type="warning"
        show-icon
        :closable="true"
        class="session-expired-alert"
        @close="clearSessionExpiredReason"
      />

      <LoginForm />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import LoginForm from '@/features/auth/components/LoginForm.vue'
import { useAuthStore } from '@/stores/auth'

const SESSION_EXPIRED_REASON_KEY = 'docflow.sessionExpiredReason'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

function getRouteReason() {
  const { reason } = route.query
  return typeof reason === 'string' && reason.trim() ? reason : null
}

function getStoredReason() {
  const reason = window.sessionStorage.getItem(SESSION_EXPIRED_REASON_KEY)
  return reason?.trim() ? reason : null
}

const sessionExpiredReason = computed(() => {
  return authStore.sessionExpiredReason || getStoredReason() || getRouteReason()
})

async function clearSessionExpiredReason() {
  authStore.sessionExpiredReason = null
  window.sessionStorage.removeItem(SESSION_EXPIRED_REASON_KEY)

  if (!('reason' in route.query)) {
    return
  }

  const nextQuery = { ...route.query }
  delete nextQuery.reason
  await router.replace({
    path: route.path,
    query: nextQuery,
  })
}
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

.session-expired-alert {
  margin-bottom: 16px;
}
</style>
