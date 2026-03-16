<template>
  <div>
    <div class="section-header">
      <h3>Folders</h3>
      <div class="header-actions">
        <span class="muted">{{ totalCount }} folders</span>
        <el-button type="primary" text @click="openCreateDialog">新增</el-button>
      </div>
    </div>

    <el-skeleton v-if="isLoading" :rows="6" animated />
    <el-alert v-else-if="error" title="Folder tree 載入失敗" type="error" show-icon :closable="false" />
    <el-empty v-else-if="!treeData.length" description="目前沒有資料夾" />

    <el-tree
      v-else
      :data="treeData"
      node-key="id"
      :props="treeProps"
      highlight-current
      default-expand-all
      @node-click="handleNodeClick"
    >
      <template #default="{ data }">
        <div class="tree-node">
          <span>{{ data.name }}</span>
          <span class="tree-actions">
            <el-button text size="small" @click.stop="openEditDialog(data)">編輯</el-button>
            <el-popconfirm title="確定刪除這個資料夾？" @confirm="handleDelete(data.id)">
              <template #reference>
                <el-button text size="small" type="danger" @click.stop>刪除</el-button>
              </template>
            </el-popconfirm>
          </span>
        </div>
      </template>
    </el-tree>

    <el-button class="all-documents-btn" text @click="handleAllDocuments">
      顯示全部文件
    </el-button>

    <FolderFormDialog v-model="createDialogVisible" :folder="null" :tree-data="treeData" />
    <FolderFormDialog v-model="editDialogVisible" :folder="editingFolder" :tree-data="treeData" />
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useMutation, useQuery, useQueryClient } from '@tanstack/vue-query'
import { ElMessage } from 'element-plus'
import { deleteFolder, getFolderTree, type FolderTreeNode } from '@/features/folder/api'
import FolderFormDialog from '@/features/folder/components/FolderFormDialog.vue'
import { useUiStore } from '@/stores/ui'

const uiStore = useUiStore()
const queryClient = useQueryClient()

const createDialogVisible = ref(false)
const editDialogVisible = ref(false)
const editingFolder = ref<FolderTreeNode | null>(null)

const { data, isLoading, error } = useQuery({
  queryKey: ['folders', 'tree'],
  queryFn: getFolderTree,
})

const deleteMutation = useMutation({
  mutationFn: deleteFolder,
  onSuccess: async () => {
    await queryClient.invalidateQueries({ queryKey: ['folders', 'tree'] })
    ElMessage.success('資料夾刪除成功')
  },
})

const treeData = computed(() => data.value ?? [])
const treeProps = {
  children: 'children',
  label: 'name',
}

const totalCount = computed(() => countNodes(treeData.value))

function handleNodeClick(node: FolderTreeNode) {
  uiStore.setSelectedFolderId(node.id)
}

function handleAllDocuments() {
  uiStore.setSelectedFolderId(null)
}

function openCreateDialog() {
  createDialogVisible.value = true
}

function openEditDialog(folder: FolderTreeNode) {
  editingFolder.value = folder
  editDialogVisible.value = true
}

async function handleDelete(folderId: number) {
  await deleteMutation.mutateAsync(folderId)
}

function countNodes(nodes: FolderTreeNode[]): number {
  return nodes.reduce((sum, node) => sum + 1 + countNodes(node.children ?? []), 0)
}
</script>

<style scoped>
.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.tree-node {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  gap: 12px;
}

.tree-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

.all-documents-btn {
  margin-top: 12px;
  padding-left: 0;
}
</style>
