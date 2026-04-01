<template>
  <AppLayout>
    <div class="dashboard-shell">
      <section class="dashboard-panel">
        <div
          ref="dashboardGridRef"
          class="dashboard-grid"
          :style="gridStyle"
        >
          <div class="side-column">
            <FolderTree />
          </div>

          <button
            v-if="isDesktop"
            type="button"
            class="column-resizer"
            role="separator"
            aria-label="調整資料夾列表與我的文件欄寬"
            aria-orientation="vertical"
            :aria-valuemin="MIN_SIDE_WIDTH"
            :aria-valuemax="maxSideWidth"
            :aria-valuenow="Math.round(sideWidth)"
            @pointerdown="startResize"
            @keydown="handleResizerKeydown"
          />

          <div class="main-column">
            <DocumentTable
              scope="mine"
              header-title="我的文件"
              header-description="顯示你建立或擁有的文件"
            />
          </div>
        </div>
      </section>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import AppLayout from '@/layouts/AppLayout.vue'
import FolderTree from '@/features/folder/components/FolderTree.vue'
import DocumentTable from '@/features/document/components/DocumentTable.vue'

const RESIZE_STORAGE_KEY = 'docflow.ui.fileManagement.sideWidth'
const DESKTOP_BREAKPOINT = 1080
const DEFAULT_SIDE_WIDTH = 340
const MIN_SIDE_WIDTH = 280
const MIN_MAIN_WIDTH = 480
const KEYBOARD_STEP = 16

const dashboardGridRef = ref<HTMLElement | null>(null)
const sideWidth = ref(readSideWidth())
const isDesktop = ref(false)
const isResizing = ref(false)
const startX = ref(0)
const startWidth = ref(DEFAULT_SIDE_WIDTH)

const maxSideWidth = computed(() => {
  const containerWidth = dashboardGridRef.value?.getBoundingClientRect().width
  if (!containerWidth) {
    return DEFAULT_SIDE_WIDTH + 220
  }

  return Math.max(MIN_SIDE_WIDTH, containerWidth - MIN_MAIN_WIDTH)
})

const gridStyle = computed(() => {
  if (!isDesktop.value) {
    return undefined
  }

  return {
    '--side-width': `${clampWidth(sideWidth.value, maxSideWidth.value)}px`,
  }
})

onMounted(() => {
  updateDesktopMode()
  window.addEventListener('resize', handleWindowResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleWindowResize)
  stopResize()
})

function readSideWidth() {
  const value = Number(localStorage.getItem(RESIZE_STORAGE_KEY))
  return Number.isFinite(value) ? value : DEFAULT_SIDE_WIDTH
}

function clampWidth(width: number, maxWidth: number) {
  return Math.min(Math.max(width, MIN_SIDE_WIDTH), maxWidth)
}

function persistWidth(width: number) {
  localStorage.setItem(RESIZE_STORAGE_KEY, String(Math.round(width)))
}

function updateDesktopMode() {
  isDesktop.value = window.innerWidth > DESKTOP_BREAKPOINT
  sideWidth.value = clampWidth(sideWidth.value, maxSideWidth.value)
}

function handleWindowResize() {
  updateDesktopMode()
}

function startResize(event: PointerEvent) {
  if (!isDesktop.value) {
    return
  }

  isResizing.value = true
  startX.value = event.clientX
  startWidth.value = sideWidth.value
  window.addEventListener('pointermove', handlePointerMove)
  window.addEventListener('pointerup', handlePointerUp)
}

function handlePointerMove(event: PointerEvent) {
  if (!isResizing.value) {
    return
  }

  const delta = event.clientX - startX.value
  sideWidth.value = clampWidth(startWidth.value + delta, maxSideWidth.value)
}

function handlePointerUp() {
  if (!isResizing.value) {
    return
  }

  persistWidth(sideWidth.value)
  stopResize()
}

function stopResize() {
  isResizing.value = false
  window.removeEventListener('pointermove', handlePointerMove)
  window.removeEventListener('pointerup', handlePointerUp)
}

function adjustSideWidth(step: number) {
  sideWidth.value = clampWidth(sideWidth.value + step, maxSideWidth.value)
  persistWidth(sideWidth.value)
}

function handleResizerKeydown(event: KeyboardEvent) {
  if (!isDesktop.value) {
    return
  }

  const step = event.shiftKey ? KEYBOARD_STEP * 2 : KEYBOARD_STEP
  if (event.key === 'ArrowLeft') {
    event.preventDefault()
    adjustSideWidth(-step)
  }

  if (event.key === 'ArrowRight') {
    event.preventDefault()
    adjustSideWidth(step)
  }
}
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

.panel-header h2 {
  margin: 0;
  font-size: 1.8rem;
}

.panel-header p {
  margin: 8px 0 0;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 0;
}

.side-column {
  min-width: 0;
}

.main-column {
  min-width: 0;
}

.column-resizer {
  display: none;
}

@media (min-width: 1081px) {
  .dashboard-grid {
    grid-template-columns: var(--side-width, 340px) 12px minmax(0, 1fr);
  }

  .side-column {
    border-right: 0;
  }

  .column-resizer {
    display: block;
    border: 0;
    border-left: 1px solid #e9eef4;
    border-right: 1px solid #e9eef4;
    background: #f8fafc;
    cursor: col-resize;
    padding: 0;
    transition: background-color 0.2s ease;
  }

  .column-resizer:hover,
  .column-resizer:focus-visible {
    background: #e2e8f0;
    outline: none;
  }
}

@media (max-width: 1080px) {
  .dashboard-grid {
    grid-template-columns: 1fr;
  }

  .side-column {
    border-right: 0;
    border-bottom: 1px solid #e9eef4;
  }

  .column-resizer {
    display: none;
  }
}

@media (max-width: 768px) {

  .panel-header h2 {
    font-size: 1.5rem;
  }
}
</style>
