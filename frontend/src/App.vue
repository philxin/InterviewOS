<script setup lang="ts">
</script>

<template>
  <div class="min-h-screen bg-gray-50">
    <nav class="bg-white shadow-sm">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="flex justify-between h-16">
          <div class="flex items-center">
            <span class="text-xl font-bold text-gray-900">InterviewOS</span>
          </div>
          <div class="flex items-center space-x-4">
            <router-link to="/" class="text-gray-700 hover:text-gray-900">首页</router-link>
            <router-link to="/interview" class="text-gray-700 hover:text-gray-900">面试</router-link>
            <router-link to="/training" class="text-gray-700 hover:text-gray-900">训练</router-link>
            <router-link to="/dashboard" class="text-gray-700 hover:text-gray-900">看板</router-link>
          </div>
        </div>
      </div>
    </nav>
    <main class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <router-view />
    </main>
  </div>
</template>

<style scoped>
</style>
