<template>
  <div class="page-shell login-page">
    <div class="page-card login-card">
      <h1>DocFlow Lite</h1>
      <p class="muted">Vue 3 前端骨架已建立。下一步接登入 API。</p>
      <el-form label-position="top">
        <el-form-item label="Username">
          <el-input placeholder="請輸入帳號" disabled />
        </el-form-item>
        <el-form-item label="Password">
          <el-input type="password" placeholder="請輸入密碼" show-password disabled />
        </el-form-item>
        <el-button type="primary" disabled>登入</el-button>
      </el-form>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  display: flex;
  align-items: center;
  justify-content: center;
}

.login-card {
  width: 100%;
  max-width: 420px;
}
</style>
