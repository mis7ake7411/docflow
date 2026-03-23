<template>
  <el-dialog
    :model-value="visible"
    title="需要更新密碼"
    width="480px"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :show-close="false"
  >
    <p class="dialog-hint">此帳號為臨時密碼，請先更新後才能繼續使用。</p>
    <el-form label-position="top">
      <el-form-item label="目前密碼">
        <el-input v-model="form.currentPassword" type="password" placeholder="請輸入目前密碼" show-password />
      </el-form-item>
      <el-form-item label="新密碼">
        <el-input v-model="form.newPassword" type="password" placeholder="請輸入新密碼" show-password />
        <p class="helper-text">至少 8 碼，需包含英文與數字。</p>
      </el-form-item>
    </el-form>

    <el-alert v-if="errorMessage" :title="errorMessage" type="error" show-icon :closable="false" class="mb-16" />

    <template #footer>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">更新密碼</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()

const submitting = ref(false)
const errorMessage = ref('')

const form = reactive({
  currentPassword: '',
  newPassword: '',
})

const visible = computed(() => Boolean(authStore.user?.mustChangePassword))

watch(visible, (value) => {
  if (value) {
    form.currentPassword = ''
    form.newPassword = ''
    errorMessage.value = ''
  }
})

function isPasswordValid(value: string) {
  return /^(?=.*[A-Za-z])(?=.*\d).{8,}$/.test(value)
}

async function handleSubmit() {
  if (!form.currentPassword || !form.newPassword) {
    errorMessage.value = '請輸入目前密碼與新密碼'
    return
  }

  if (!isPasswordValid(form.newPassword)) {
    errorMessage.value = '密碼至少 8 碼，且需包含英文與數字'
    return
  }

  submitting.value = true
  errorMessage.value = ''
  try {
    await authStore.changePassword({
      currentPassword: form.currentPassword,
      newPassword: form.newPassword,
    })
    ElMessage.success('密碼已更新')
  } catch {
    errorMessage.value = '更新失敗，請確認目前密碼是否正確'
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.dialog-hint {
  margin: 0 0 16px;
  color: #6b7280;
}

.mb-16 {
  margin-bottom: 16px;
}

.helper-text {
  margin: 6px 0 0;
  color: #6b7280;
  font-size: 0.9rem;
}
</style>
