<template>
  <div class="folder-panel">
    <div class="section-header">
      <div>
        <h3>資料夾列表</h3>
        <p class="muted">共 {{ totalCount }} 個資料夾</p>
      </div>
      <el-button v-if="!isManager" type="primary" @click="openCreateDialog">新增資料夾</el-button>
    </div>

    <div class="folder-content">
      <el-skeleton v-if="isLoading" :rows="6" animated />
      <el-alert v-else-if="error" title="資料夾樹載入失敗" type="error" show-icon :closable="false" />
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
            <span class="node-name">{{ data.name }}</span>
            <span v-if="!isManager" class="tree-actions">
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
    </div>

    <div class="panel-footer">
      <button type="button" class="footer-link" @click="handleAllDocuments">顯示全部文件</button>
    </div>

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
import { useAuthStore } from '@/stores/auth'

const uiStore = useUiStore()
const authStore = useAuthStore()
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
    ElMessage.success('資料夾已刪除')
  },
})

const treeData = computed(() => data.value ?? [])
const treeProps = {
  children: 'children',
  label: 'name',
}

const totalCount = computed(() => countNodes(treeData.value))
const isManager = computed(() => authStore.userRole === 'MANAGER')

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
.folder-panel {
  display: flex;
  flex-direction: column;
  min-height: 100%;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 24px 24px 18px;
  border-bottom: 1px solid #edf2f7;
}

.section-header h3,
.section-header p {
  margin: 0;
}

.section-header p {
  margin-top: 8px;
}

.folder-content {
  flex: 1;
  padding: 20px 24px;
}

.tree-node {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  gap: 12px;
}

.node-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tree-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

.panel-footer {
  padding: 0 24px 20px;
}

.footer-link {
  border: 0;
  background: transparent;
  color: #26b3bc;
  cursor: pointer;
  padding: 0;
  font: inherit;
}

@media (max-width: 768px) {
  .section-header {
    flex-direction: column;
    align-items: stretch;
    padding: 16px;
  }

  .folder-content,
  .panel-footer {
    padding-inline: 16px;
  }
}
</style>

