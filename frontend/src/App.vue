<script setup lang="ts">
</script>

<template>
  <div class="app-shell">
    <nav class="top-nav">
      <div class="top-nav-inner">
        <div class="brand">InterviewOS</div>
        <div class="nav-links">
          <router-link to="/">知识点</router-link>
          <router-link to="/knowledge/new">新建</router-link>
          <router-link to="/history">历史</router-link>
        </div>
      </div>
    </nav>
    <main class="content">
      <router-view />
    </main>
  </div>
</template>

<style scoped>
.app-shell {
  min-height: 100vh;
  background: linear-gradient(180deg, #f8fafc 0%, #eef2ff 100%);
}

.top-nav {
  border-bottom: 1px solid #e2e8f0;
  background: #ffffffcc;
  backdrop-filter: blur(8px);
}

.top-nav-inner {
  max-width: 1000px;
  margin: 0 auto;
  height: 64px;
  padding: 0 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.brand {
  font-size: 20px;
  font-weight: 700;
  color: #0f172a;
}

.nav-links {
  display: flex;
  gap: 16px;
}

.nav-links a {
  color: #334155;
  font-weight: 600;
}

.nav-links a.router-link-active {
  color: #1d4ed8;
}

.content {
  max-width: 1000px;
  margin: 0 auto;
  padding: 24px 20px 40px;
}
</style>
