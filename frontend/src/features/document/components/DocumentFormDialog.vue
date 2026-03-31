<template>
  <el-dialog
    :model-value="modelValue"
    :title="isEdit ? '編輯文件' : '新增文件'"
    width="520px"
    @close="emit('update:modelValue', false)"
  >
    <el-form label-position="top">
      <el-form-item label="標題">
        <el-input v-model="form.title" placeholder="請輸入文件標題" />
      </el-form-item>

      <el-form-item label="描述">
        <el-input v-model="form.description" type="textarea" :rows="4" placeholder="請輸入文件描述" />
      </el-form-item>

      <el-form-item label="狀態">
        <el-select v-model="form.status" style="width: 100%">
          <el-option label="草稿" value="DRAFT" />
          <el-option label="啟用" value="ACTIVE" />
          <el-option label="封存" value="ARCHIVED" />
        </el-select>
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="emit('update:modelValue', false)">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">確認</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { useMutation, useQueryClient } from '@tanstack/vue-query'
import { ElMessage } from 'element-plus'
import { createDocument, updateDocument, type DocumentItem } from '@/features/document/api'
import { useUiStore } from '@/stores/ui'
import { PERMISSION_MESSAGES } from '@/shared/utils/permission'
import { isAxiosError } from 'axios'

const props = defineProps<{
  modelValue: boolean
  document?: DocumentItem | null
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
}>()

const uiStore = useUiStore()
const queryClient = useQueryClient()
const submitting = ref(false)

const form = reactive({
  title: '',
  description: '',
  status: 'DRAFT',
})

const isEdit = ref(false)

watch(
  () => props.document,
  (document) => {
    if (document) {
      isEdit.value = true
      form.title = document.title
      form.description = document.description ?? ''
      form.status = document.status
    } else {
      isEdit.value = false
      form.title = ''
      form.description = ''
      form.status = 'DRAFT'
    }
  },
  { immediate: true },
)

const createMutation = useMutation({
  mutationFn: createDocument,
  onSuccess: async () => {
    await queryClient.invalidateQueries({ queryKey: ['documents'] })
    ElMessage.success('文件已建立')
    emit('update:modelValue', false)
  },
  onError: (error) => {
    if (isAxiosError(error) && error.response?.status === 403) {
      ElMessage.error(PERMISSION_MESSAGES.documentForbidden)
    }
  },
})

const updateMutation = useMutation({
  mutationFn: ({ id, payload }: { id: number; payload: { folderId: number | null; title: string; description: string; status: string } }) =>
    updateDocument(id, payload),
  onSuccess: async (_, variables) => {
    await queryClient.invalidateQueries({ queryKey: ['documents'] })
    await queryClient.invalidateQueries({ queryKey: ['documents', 'detail', variables.id] })
    ElMessage.success('文件已更新')
    emit('update:modelValue', false)
  },
  onError: (error) => {
    if (isAxiosError(error) && error.response?.status === 403) {
      ElMessage.error(PERMISSION_MESSAGES.documentForbidden)
    }
  },
})

async function handleSubmit() {
  if (!form.title.trim()) {
    ElMessage.error('請輸入文件標題')
    return
  }

  submitting.value = true
  try {
    const payload = {
      folderId: props.document ? props.document.folderId : uiStore.selectedFolderId,
      title: form.title.trim(),
      description: form.description.trim(),
      status: form.status,
    }

    if (props.document) {
      await updateMutation.mutateAsync({ id: props.document.id, payload })
    } else {
      await createMutation.mutateAsync(payload)
    }
  } finally {
    submitting.value = false
  }
}
</script>
