<template>
  <el-dialog :model-value="modelValue" title="上傳檔案" width="480px" @close="emit('update:modelValue', false)">
    <el-upload
      drag
      :auto-upload="false"
      :show-file-list="true"
      :limit="1"
      :on-change="handleChange"
      :on-remove="handleRemove"
    >
      <el-icon><upload-filled /></el-icon>
      <div class="el-upload__text">將檔案拖曳到這裡，或點擊選擇檔案</div>
    </el-upload>

    <template #footer>
      <el-button @click="emit('update:modelValue', false)">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">上傳</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import type { UploadFile } from 'element-plus'
import { uploadDocumentFile } from '@/features/document/api'
import { useMutation, useQueryClient } from '@tanstack/vue-query'

const props = defineProps<{
  modelValue: boolean
  documentId: number
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
}>()

const queryClient = useQueryClient()
const selectedFile = ref<File | null>(null)
const submitting = ref(false)

const uploadMutation = useMutation({
  mutationFn: ({ id, file }: { id: number; file: File }) => uploadDocumentFile(id, file),
  onSuccess: async (_, variables) => {
    await queryClient.invalidateQueries({ queryKey: ['documents', 'list'] })
    await queryClient.invalidateQueries({ queryKey: ['documents', 'detail', variables.id] })
    ElMessage.success('檔案上傳成功')
    emit('update:modelValue', false)
    selectedFile.value = null
  },
})

function handleChange(file: UploadFile) {
  selectedFile.value = file.raw ?? null
}

function handleRemove() {
  selectedFile.value = null
}

async function handleSubmit() {
  if (!selectedFile.value) {
    ElMessage.error('請先選擇檔案')
    return
  }

  submitting.value = true
  try {
    await uploadMutation.mutateAsync({ id: props.documentId, file: selectedFile.value })
  } finally {
    submitting.value = false
  }
}
</script>
