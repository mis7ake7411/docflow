<template>
  <AppLayout>
    <div class="dashboard-shell">
      <section class="dashboard-panel">
        <div class="panel-header">
          <div>
            <h2>文件工作區</h2>
            <p class="muted">以後台清單版型集中處理資料夾與文件。</p>
          </div>
          <div class="panel-tools">
            <button type="button" class="tool-button">刷新</button>
            <button type="button" class="tool-button">設定</button>
          </div>
        </div>

        <div class="dashboard-grid">
          <div class="side-column">
            <FolderTree />
          </div>

          <div class="main-column">
            <DocumentTable />
          </div>
        </div>
      </section>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import AppLayout from '@/layouts/AppLayout.vue'
import FolderTree from '@/features/folder/components/FolderTree.vue'
import DocumentTable from '@/features/document/components/DocumentTable.vue'
</script>

<style scoped>
.dashboard-shell {
  display: flex;
  flex-direction: column;
}

.dashboard-panel {
  background: #ffffff;
  border: 1px solid #e5ebf1;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.04);
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 24px 28px;
  border-bottom: 1px solid #e9eef4;
}

.panel-header h2 {
  margin: 0;
  font-size: 1.8rem;
}

.panel-header p {
  margin: 8px 0 0;
}

.panel-tools {
  display: flex;
  align-items: center;
  gap: 10px;
}

.tool-button {
  border: 1px solid #d9e1ea;
  background: #fff;
  color: #526071;
  padding: 10px 14px;
  cursor: pointer;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: 340px minmax(0, 1fr);
  gap: 0;
}

.side-column {
  border-right: 1px solid #e9eef4;
}

.main-column {
  min-width: 0;
}

@media (max-width: 1080px) {
  .dashboard-grid {
    grid-template-columns: 1fr;
  }

  .side-column {
    border-right: 0;
    border-bottom: 1px solid #e9eef4;
  }
}

@media (max-width: 768px) {
  .panel-header {
    flex-direction: column;
    align-items: stretch;
    padding: 18px 16px;
  }

  .panel-header h2 {
    font-size: 1.5rem;
  }
}
</style>
