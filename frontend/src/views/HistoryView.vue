<template>
  <section class="history-page">
    <header class="page-header">
      <div>
        <h1>训练历史</h1>
        <p>查看每次训练的结果摘要，并按知识点筛选。</p>
      </div>
      <button class="btn" type="button" @click="refresh">刷新</button>
    </header>

    <section class="card filter-card">
      <label>
        <span>知识点筛选</span>
        <select v-model.number="selectedKnowledgeId" @change="onFilterChange">
          <option :value="0">全部知识点</option>
          <option v-for="item in knowledgeList" :key="item.id" :value="item.id">{{ item.title }}</option>
        </select>
      </label>

      <label>
        <span>每页条数</span>
        <select v-model.number="size" @change="onFilterChange">
          <option :value="10">10</option>
          <option :value="20">20</option>
          <option :value="50">50</option>
        </select>
      </label>
    </section>

    <div v-if="loading" class="card state-card">正在加载训练历史...</div>
    <div v-else-if="errorMessage" class="card state-card error">{{ errorMessage }}</div>
    <div v-else-if="sessions.length === 0" class="card state-card">暂无训练记录。</div>

    <div v-else class="history-list">
      <article v-for="item in sessions" :key="item.sessionId" class="card history-card">
        <header class="history-header">
          <div>
            <h2>{{ item.knowledgeTitle }}</h2>
            <p class="meta">
              {{ formatDateTime(item.startedAt) }}
              <span v-if="item.completedAt"> · 完成于 {{ formatDateTime(item.completedAt) }}</span>
            </p>
          </div>
          <div class="session-side">
            <span v-if="item.band" class="band-pill">{{ item.band.label }}</span>
            <strong class="score-pill">{{ item.sessionScore }}</strong>
          </div>
        </header>

        <p class="summary">{{ item.majorIssueSummary || '暂无问题摘要。' }}</p>
        <div class="card-footer">
          <span class="meta">已答 {{ item.answeredCount }} / {{ item.questionCount }} 题</span>
          <button class="btn btn-primary" type="button" @click="goResult(item.sessionId)">查看详情</button>
        </div>
      </article>
    </div>

    <footer v-if="total > 0" class="pagination">
      <button class="btn" :disabled="page <= 1 || loading" type="button" @click="goPrev">上一页</button>
      <span>第 {{ page }} 页 · 共 {{ total }} 条</span>
      <button class="btn" :disabled="!hasNext || loading" type="button" @click="goNext">下一页</button>
    </footer>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { knowledgeAPI, trainingAPI } from '../api'
import type { Knowledge, TrainingSessionSummary } from '../types'
import { formatDateTime } from '../utils/date'

const router = useRouter()

const loading = ref(false)
const errorMessage = ref('')
const selectedKnowledgeId = ref(0)
const page = ref(1)
const size = ref(10)
const total = ref(0)
const hasNext = ref(false)
const knowledgeList = ref<Knowledge[]>([])
const sessions = ref<TrainingSessionSummary[]>([])

async function refresh() {
  loading.value = true
  errorMessage.value = ''
  try {
    const [knowledge, sessionPage] = await Promise.all([
      knowledgeAPI.getList(),
      trainingAPI.getSessions({
        page: page.value,
        size: size.value,
        knowledgeId: selectedKnowledgeId.value || undefined,
      }),
    ])
    knowledgeList.value = knowledge
    sessions.value = sessionPage.items
    total.value = sessionPage.total
    hasNext.value = sessionPage.hasNext
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '加载历史失败'
  } finally {
    loading.value = false
  }
}

function onFilterChange() {
  page.value = 1
  refresh()
}

function goPrev() {
  if (page.value <= 1) {
    return
  }
  page.value -= 1
  refresh()
}

function goNext() {
  if (!hasNext.value) {
    return
  }
  page.value += 1
  refresh()
}

function goResult(sessionId: string) {
  router.push(`/result/${sessionId}`)
}

onMounted(refresh)
</script>

<style scoped>
.history-page {
  display: grid;
  gap: 16px;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.page-header h1 {
  margin: 0;
  font-size: 28px;
}

.page-header p {
  margin: 6px 0 0;
  color: #64748b;
}

.filter-card {
  padding: 14px 16px;
  display: flex;
  gap: 14px;
  align-items: end;
}

label {
  display: grid;
  gap: 6px;
}

label span {
  font-size: 13px;
  font-weight: 700;
  color: #475569;
}

select {
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  padding: 8px 10px;
  min-width: 200px;
}

.state-card {
  padding: 20px;
}

.error {
  color: #b91c1c;
  border-color: #fecaca;
  background: #fff1f2;
}

.history-list {
  display: grid;
  gap: 12px;
}

.history-card {
  padding: 16px;
}

.history-header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.history-header h2 {
  margin: 0;
  font-size: 18px;
}

.meta {
  color: #64748b;
  font-size: 13px;
}

.session-side {
  display: flex;
  align-items: center;
  gap: 8px;
}

.band-pill,
.score-pill {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.band-pill {
  background: #eff6ff;
  color: #1d4ed8;
}

.score-pill {
  background: #0f172a;
  color: #fff;
}

.summary {
  margin: 14px 0 0;
  color: #334155;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-top: 14px;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 10px;
}

@media (max-width: 768px) {
  .page-header,
  .filter-card,
  .history-header,
  .card-footer,
  .pagination {
    flex-direction: column;
    align-items: flex-start;
  }

  select {
    min-width: 220px;
  }
}
</style>
