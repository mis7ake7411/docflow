<template>
  <el-form ref="formRef" :model="form" label-position="top" @submit.prevent="handleSubmit">
    <el-form-item label="帳號">
      <el-input v-model="form.username" placeholder="請輸入帳號" />
    </el-form-item>

    <el-form-item label="Email">
      <el-input v-model="form.email" placeholder="請輸入 Email" />
    </el-form-item>

    <el-form-item label="密碼">
      <el-input v-model="form.password" type="password" placeholder="請輸入密碼" show-password />
      <p class="helper-text">至少 8 碼，需包含英文與數字。</p>
    </el-form-item>

    <el-alert v-if="errorMessage" :title="errorMessage" type="error" show-icon :closable="false" class="mb-16" />

    <el-button type="primary" :loading="submitting" @click="handleSubmit">建立帳號</el-button>
  </el-form>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const formRef = ref()
const submitting = ref(false)
const errorMessage = ref('')

const form = reactive({
  username: '',
  email: '',
  password: '',
})

function isEmailValid(value: string) {
  return /.+@.+\..+/.test(value)
}

function isPasswordValid(value: string) {
  return /^(?=.*[A-Za-z])(?=.*\d).{8,}$/.test(value)
}

async function handleSubmit() {
  if (!form.username.trim() || !form.email.trim() || !form.password) {
    errorMessage.value = '請完整填寫帳號、Email 與密碼'
    return
  }

  if (!isEmailValid(form.email)) {
    errorMessage.value = 'Email 格式不正確'
    return
  }

  if (!isPasswordValid(form.password)) {
    errorMessage.value = '密碼至少 8 碼，且需包含英文與數字'
    return
  }

  submitting.value = true
  errorMessage.value = ''

  try {
    await authStore.register({
      username: form.username.trim(),
      email: form.email.trim(),
      password: form.password,
    })
    ElMessage.success('註冊成功')
    await router.push('/app')
  } catch {
    errorMessage.value = '註冊失敗，請確認資料是否正確。'
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.mb-16 {
  margin-bottom: 16px;
}

.helper-text {
  margin: 6px 0 0;
  color: #6b7280;
  font-size: 0.9rem;
}
</style>
