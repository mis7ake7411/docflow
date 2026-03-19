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
  gap: 10px;
}

.title-block h1 {
  margin: 0;
  font-size: 1.9rem;
  line-height: 1.15;
}

.title-block .muted {
  margin: 8px 0 0;
}

@media (max-width: 768px) {
  .title-block h1 {
    font-size: 1.5rem;
  }
}
</style>
