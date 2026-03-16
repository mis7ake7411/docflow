<template>
  <el-dialog :model-value="modelValue" :title="isEdit ? '編輯文件' : '新增文件'" width="520px" @close="emit('update:modelValue', false)">
    <el-form label-position="top">
      <el-form-item label="Title">
        <el-input v-model="form.title" placeholder="輸入文件標題" />
      </el-form-item>

      <el-form-item label="Description">
        <el-input v-model="form.description" type="textarea" :rows="4" placeholder="輸入描述" />
      </el-form-item>

      <el-form-item label="Status">
        <el-select v-model="form.status" style="width: 100%">
          <el-option label="DRAFT" value="DRAFT" />
          <el-option label="ACTIVE" value="ACTIVE" />
          <el-option label="ARCHIVED" value="ARCHIVED" />
        </el-select>
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="emit('update:modelValue', false)">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">儲存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { reactive, watch, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { createDocument, updateDocument, type DocumentItem } from '@/features/document/api'
import { useMutation, useQueryClient } from '@tanstack/vue-query'
import { useUiStore } from '@/stores/ui'

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
    await queryClient.invalidateQueries({ queryKey: ['documents', 'list'] })
    ElMessage.success('文件建立成功')
    emit('update:modelValue', false)
  },
})

const updateMutation = useMutation({
  mutationFn: ({ id, payload }: { id: number; payload: { folderId: number | null; title: string; description: string; status: string } }) => updateDocument(id, payload),
  onSuccess: async (_, variables) => {
    await queryClient.invalidateQueries({ queryKey: ['documents', 'list'] })
    await queryClient.invalidateQueries({ queryKey: ['documents', 'detail', variables.id] })
    ElMessage.success('文件更新成功')
    emit('update:modelValue', false)
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
      folderId: uiStore.selectedFolderId,
      title: form.title,
      description: form.description,
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
