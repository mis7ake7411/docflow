<template>
  <div class="page-header">
    <el-breadcrumb separator="/">
      <el-breadcrumb-item
        v-for="item in breadcrumbItems"
        :key="item"
      >
        {{ item }}
      </el-breadcrumb-item>
    </el-breadcrumb>

    <div class="title-block">
      <p class="eyebrow">Workspace</p>
      <h1>{{ title }}</h1>
      <p class="muted">{{ subtitle }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  title: string
  subtitle: string
  breadcrumbItems: string[]
}>()
</script>

<style scoped>
.page-header {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.title-block h1 {
  margin: 4px 0 8px;
  font-size: 2rem;
}

.eyebrow {
  margin: 0;
  font-size: 0.8rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #2563eb;
}

@media (max-width: 768px) {
  .title-block h1 {
    font-size: 1.6rem;
  }
}
</style>
