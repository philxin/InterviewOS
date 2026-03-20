<template>
  <section class="history-page">
    <header class="page-header">
      <div>
        <h1>训练历史</h1>
        <p>查看每次训练的结果摘要，并按知识点筛选。</p>
      </div>
      <button class="btn" type="button" @click="refresh">🔄 刷新</button>
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

    <AppStateCard v-if="loading" variant="loading" message="正在加载训练历史..." />
    <AppStateCard v-else-if="errorMessage" variant="error" :message="errorMessage" />
    <AppStateCard v-else-if="sessions.length === 0" variant="empty" message="暂无训练记录。" />

    <div v-else class="history-list">
      <article v-for="item in sessions" :key="item.sessionId" class="card history-card">
        <header class="history-header">
          <div>
            <h2>{{ item.knowledgeTitle }}</h2>
            <p class="meta-text">
              {{ formatDateTime(item.startedAt) }}
              <span v-if="item.completedAt"> · 完成于 {{ formatDateTime(item.completedAt) }}</span>
            </p>
          </div>
          <div class="session-side">
            <span v-if="item.band" class="pill pill-blue">{{ item.band.label }}</span>
            <strong class="pill pill-dark">{{ item.sessionScore }}</strong>
          </div>
        </header>

        <p class="summary">{{ item.majorIssueSummary || '暂无问题摘要。' }}</p>
        <div class="card-footer">
          <span class="meta-text">已答 {{ item.answeredCount }} / {{ item.questionCount }} 题</span>
          <button class="btn btn-primary" type="button" @click="goResult(item.sessionId)">查看详情</button>
        </div>
      </article>
    </div>

    <footer v-if="total > 0" class="pagination">
      <button class="btn" :disabled="page <= 1 || loading" type="button" @click="goPrev">← 上一页</button>
      <span class="page-info">第 {{ page }} 页 · 共 {{ total }} 条</span>
      <button class="btn" :disabled="!hasNext || loading" type="button" @click="goNext">下一页 →</button>
    </footer>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { knowledgeAPI, trainingAPI } from '../api'
import AppStateCard from '../components/AppStateCard.vue'
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
  if (page.value <= 1) return
  page.value -= 1
  refresh()
}

function goNext() {
  if (!hasNext.value) return
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
  gap: var(--sp-5);
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--sp-3);
}

.page-header h1 {
  margin: 0;
  font-size: var(--fs-3xl);
  font-weight: 800;
  letter-spacing: -0.02em;
}

.page-header p {
  margin: var(--sp-1) 0 0;
  color: var(--clr-text-secondary);
  font-size: var(--fs-sm);
}

.filter-card {
  padding: var(--sp-4) var(--sp-5);
  display: flex;
  gap: var(--sp-4);
  align-items: end;
}

label {
  display: grid;
  gap: 6px;
}

label span {
  font-size: var(--fs-sm);
  font-weight: 700;
  color: var(--clr-text-secondary);
}

select {
  border: 1.5px solid var(--clr-border);
  border-radius: var(--radius-sm);
  padding: 10px 14px;
  min-width: 200px;
  background: var(--clr-surface);
  font-size: var(--fs-sm);
  transition: all var(--duration-fast) var(--ease-out);
  cursor: pointer;
}

select:focus {
  outline: none;
  border-color: var(--clr-primary);
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.1);
}

.history-list {
  display: grid;
  gap: var(--sp-3);
}

.history-card {
  padding: var(--sp-5);
  border-left: 3px solid transparent;
  transition: all var(--duration-normal) var(--ease-out);
}

.history-card:hover {
  border-left-color: var(--clr-primary);
  transform: translateX(2px);
}

.history-header {
  display: flex;
  justify-content: space-between;
  gap: var(--sp-3);
}

.history-header h2 {
  margin: 0;
  font-size: var(--fs-lg);
  font-weight: 700;
}

.meta-text {
  color: var(--clr-text-tertiary);
  font-size: var(--fs-xs);
  margin: var(--sp-1) 0 0;
}

.session-side {
  display: flex;
  align-items: center;
  gap: var(--sp-2);
  flex-shrink: 0;
}

.pill {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 4px 12px;
  border-radius: var(--radius-full);
  font-size: var(--fs-xs);
  font-weight: 700;
}

.pill-blue {
  background: var(--clr-primary-50);
  color: var(--clr-primary-dark);
}

.pill-dark {
  background: var(--clr-text);
  color: var(--clr-text-inverse);
}

.summary {
  margin: var(--sp-3) 0 0;
  color: var(--clr-text-secondary);
  font-size: var(--fs-sm);
  line-height: 1.6;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--sp-3);
  margin-top: var(--sp-4);
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: var(--sp-4);
}

.page-info {
  font-size: var(--fs-sm);
  color: var(--clr-text-secondary);
  font-weight: 600;
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
  }
  .filter-card {
    flex-direction: column;
    align-items: stretch;
  }
  select {
    min-width: 0;
    width: 100%;
  }
  .history-header {
    flex-direction: column;
  }
  .card-footer {
    flex-direction: column;
    align-items: flex-start;
  }
  .card-footer .btn {
    width: 100%;
  }
}
</style>
