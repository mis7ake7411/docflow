<template>
  <el-form ref="formRef" :model="form" label-position="top" @submit.prevent="handleSubmit">
    <el-form-item label="帳號">
      <el-input
        v-model="form.username"
        placeholder="請輸入帳號"
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

    <el-alert v-if="errorMessage" :title="errorMessage" type="error" show-icon :closable="false" class="mb-16" />

    <el-button type="primary" native-type="submit" :loading="submitting">登入</el-button>

    <div class="form-footer">
      <span>還沒有帳號？</span>
      <RouterLink class="form-link" to="/register">註冊</RouterLink>
    </div>
  </el-form>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { InputInstance } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const formRef = ref()
const passwordRef = ref<InputInstance>()
const submitting = ref(false)
const errorMessage = ref('')

const form = reactive({
  username: '',
  password: '',
})

/** 帳號欄按 Enter 時將焦點移至密碼欄 */
function focusPassword() {
  passwordRef.value?.focus()
}

async function handleSubmit() {
  if (!form.username || !form.password) {
    errorMessage.value = '請輸入帳號與密碼'
    return
  }

  submitting.value = true
  errorMessage.value = ''

  try {
    await authStore.login({
      username: form.username,
      password: form.password,
    })
    ElMessage.success('登入成功')
    await router.push('/app')
  } catch {
    errorMessage.value = '登入失敗，請確認帳號密碼是否正確。'
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
