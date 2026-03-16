<template>
  <div>
    <div class="section-header">
      <h3>Folders</h3>
      <span class="muted">{{ totalCount }} folders</span>
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
        </div>
      </template>
    </el-tree>

    <el-button class="all-documents-btn" text @click="handleAllDocuments">
      顯示全部文件
    </el-button>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useQuery } from '@tanstack/vue-query'
import { getFolderTree, type FolderTreeNode } from '@/features/folder/api'
import { useUiStore } from '@/stores/ui'

const uiStore = useUiStore()

const { data, isLoading, error } = useQuery({
  queryKey: ['folders', 'tree'],
  queryFn: getFolderTree,
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

.tree-node {
  display: flex;
  align-items: center;
  gap: 8px;
}

.all-documents-btn {
  margin-top: 12px;
  padding-left: 0;
}
</style>
