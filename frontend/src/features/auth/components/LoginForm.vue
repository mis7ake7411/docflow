<template>
  <el-form ref="formRef" :model="form" label-position="top" @submit.prevent="handleSubmit">
    <el-form-item label="使用者名稱">
      <el-input
        v-model="form.username"
        placeholder="請輸入使用者名稱"
        @keyup.enter="focusPassword"
      />
    </el-form-item>

    <el-form-item label="密碼">
      <el-input
        ref="passwordRef"
        v-model="form.password"
        type="password"
        placeholder="請輸入密碼"
        show-password
        @keyup.enter.prevent="handleSubmit"
      />
    </el-form-item>

    <el-alert
      v-if="errorMessage"
      :title="errorMessage"
      type="error"
      show-icon
      :closable="false"
      class="mb-16"
    />

    <el-button type="primary" native-type="submit" :loading="submitting">登入</el-button>

    <div class="form-footer">
      <span>還沒有帳號？</span>
      <RouterLink class="form-link" to="/register">註冊</RouterLink>
    </div>
  </el-form>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { InputInstance } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const SESSION_EXPIRED_REASON_KEY = 'docflow.sessionExpiredReason'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const formRef = ref()
const passwordRef = ref<InputInstance>()
const submitting = ref(false)
const errorMessage = ref('')

const form = reactive({
  username: '',
  password: '',
})

function resolveRedirectPath(redirect: unknown) {
  if (typeof redirect !== 'string' || !redirect) {
    return '/app'
  }

  let decoded = redirect

  try {
    decoded = decodeURIComponent(redirect)
  } catch {
    return '/app'
  }

  if (!decoded.startsWith('/') || decoded.startsWith('//')) {
    return '/app'
  }

  return decoded
}

const redirectPath = computed(() => resolveRedirectPath(route.query.redirect))

function clearSessionExpiredState() {
  authStore.sessionExpiredReason = null
  window.sessionStorage.removeItem(SESSION_EXPIRED_REASON_KEY)
}

function focusPassword() {
  passwordRef.value?.focus()
}

async function handleSubmit() {
  if (!form.username || !form.password) {
    errorMessage.value = '請輸入使用者名稱與密碼'
    return
  }

  submitting.value = true
  errorMessage.value = ''

  try {
    await authStore.login({
      username: form.username,
      password: form.password,
    })

    clearSessionExpiredState()
    ElMessage.success('登入成功')
    await router.push(redirectPath.value)
  } catch {
    errorMessage.value = '登入失敗，請確認帳號密碼後再試一次'
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.mb-16 {
  margin-bottom: 16px;
}

.form-footer {
  margin-top: 16px;
  display: flex;
  align-items: center;
  gap: 8px;
  color: #6b7280;
  font-size: 0.95rem;
}

.form-link {
  color: #2563eb;
  text-decoration: none;
  font-weight: 600;
}

.form-link:hover {
  text-decoration: underline;
}
</style>
