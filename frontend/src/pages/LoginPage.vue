<template>
  <div class="page-shell login-page">
    <div class="page-card login-card">
      <h1>DocFlow Lite</h1>
      <p class="muted">登入後即可管理文件、資料夾與活動紀錄。</p>

      <!-- 顯示 Session 過期提示 -->
      <el-alert
        v-if="sessionExpiredReason"
        :title="sessionExpiredReason"
        type="warning"
        show-icon
        :closable="true"
        class="session-expired-alert"
      />

      <LoginForm />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import LoginForm from '@/features/auth/components/LoginForm.vue'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const authStore = useAuthStore()

// 優先使用 auth store 中的過期原因，或從 URL 查詢參數讀取
const sessionExpiredReason = computed(() => {
  return authStore.sessionExpiredReason || (route.query.reason ? String(route.query.reason) : null)
})

// 登入後若有重定向路徑，保存供登入成功後使用
const redirectPath = ref<string | null>(null)

onMounted(() => {
  if (route.query.redirect) {
    redirectPath.value = String(route.query.redirect)
  }
})
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
