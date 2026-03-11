<template>
  <section class="dashboard-page">
    <header class="page-header">
      <div>
        <h1>知识点训练看板</h1>
        <p>管理知识点并发起 AI 训练。</p>
      </div>
      <div class="actions">
        <button class="btn" type="button" @click="goHistory">训练历史</button>
        <button class="btn btn-primary" type="button" @click="goCreate">+ 新建知识点</button>
      </div>
    </header>

    <div v-if="loading" class="card state-card">正在加载知识点...</div>
    <div v-else-if="errorMessage" class="card state-card error">{{ errorMessage }}</div>
    <div v-else-if="knowledgeList.length === 0" class="card state-card">暂无知识点，请先新建。</div>

    <div v-else class="knowledge-list">
      <article v-for="item in knowledgeList" :key="item.id" class="card knowledge-card">
        <header class="knowledge-header">
          <h2>{{ item.title }}</h2>
          <MasteryBadge :mastery="item.mastery" />
        </header>
        <p class="knowledge-content">{{ item.content }}</p>
        <footer class="knowledge-footer">
          <span class="meta">创建于 {{ formatDateTime(item.createdAt) }}</span>
          <div class="actions">
            <button class="btn btn-primary" type="button" @click="goTraining(item.id)">开始训练</button>
            <button class="btn" type="button" @click="goEdit(item.id)">编辑</button>
            <button class="btn btn-danger" type="button" @click="onDelete(item.id)">删除</button>
          </div>
        </footer>
      </article>
    </div>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import MasteryBadge from '../components/MasteryBadge.vue'
import { knowledgeAPI } from '../api'
import type { Knowledge } from '../types'
import { formatDateTime } from '../utils/date'

const router = useRouter()

const loading = ref(false)
const errorMessage = ref('')
const knowledgeList = ref<Knowledge[]>([])

async function fetchKnowledgeList() {
  loading.value = true
  errorMessage.value = ''
  try {
    knowledgeList.value = await knowledgeAPI.getList()
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '获取知识点失败'
  } finally {
    loading.value = false
  }
}

function goCreate() {
  router.push('/knowledge/new')
}

function goEdit(id: number) {
  router.push(`/knowledge/edit/${id}`)
}

function goTraining(id: number) {
  router.push(`/training/${id}`)
}

function goHistory() {
  router.push('/history')
}

async function onDelete(id: number) {
  if (!window.confirm('确认删除该知识点？删除后训练记录也会被删除。')) {
    return
  }
  try {
    await knowledgeAPI.delete(id)
    await fetchKnowledgeList()
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '删除失败'
  }
}

onMounted(fetchKnowledgeList)
</script>

<style scoped>
.dashboard-page {
  display: grid;
  gap: 16px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.page-header h1 {
  margin: 0;
  font-size: 28px;
  line-height: 1.2;
}

.page-header p {
  margin: 6px 0 0;
  color: #64748b;
}

.actions {
  display: flex;
  gap: 8px;
}

.state-card {
  padding: 20px;
}

.error {
  color: #b91c1c;
  border-color: #fecaca;
  background: #fff1f2;
}

.knowledge-list {
  display: grid;
  gap: 12px;
}

.knowledge-card {
  padding: 16px;
}

.knowledge-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.knowledge-header h2 {
  margin: 0;
  font-size: 20px;
}

.knowledge-content {
  color: #334155;
  margin: 10px 0 14px;
  white-space: pre-wrap;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
}

.knowledge-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.meta {
  color: #64748b;
  font-size: 13px;
}

@media (max-width: 768px) {
  .page-header,
  .knowledge-footer {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
