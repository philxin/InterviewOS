<template>
  <section class="history-page">
    <header class="page-header">
      <h1>训练历史</h1>
      <p>按知识点查看历次训练表现。</p>
    </header>

    <section class="card filter-card">
      <label>
        <span>知识点筛选</span>
        <select v-model.number="selectedKnowledgeId">
          <option :value="0">全部</option>
          <option v-for="item in knowledgeList" :key="item.id" :value="item.id">{{ item.title }}</option>
        </select>
      </label>
      <button class="btn" type="button" @click="refresh">刷新</button>
    </section>

    <div v-if="loading" class="card state-card">正在加载训练历史...</div>
    <div v-else-if="errorMessage" class="card state-card error">{{ errorMessage }}</div>
    <div v-else-if="filteredRecords.length === 0" class="card state-card">暂无训练记录。</div>

    <div v-else class="history-list">
      <article v-for="record in filteredRecords" :key="record.id" class="card history-card">
        <header>
          <h2>{{ knowledgeTitleMap[record.knowledgeId] || `知识点 #${record.knowledgeId}` }}</h2>
          <span class="overall">综合 {{ record.overall }}</span>
        </header>
        <p class="meta">{{ formatDateTime(record.createdAt) }}</p>
        <p class="question">Q：{{ record.question }}</p>
        <p class="answer">A：{{ record.answer }}</p>
        <details>
          <summary>查看详细评分</summary>
          <div class="detail">
            <p>准确度：{{ record.accuracy }}</p>
            <p>深度：{{ record.depth }}</p>
            <p>清晰度：{{ record.clarity }}</p>
            <p>优点：{{ record.strengths || '暂无' }}</p>
            <p>待改进：{{ record.weaknesses || '暂无' }}</p>
            <ul v-if="record.suggestions.length > 0">
              <li v-for="(item, index) in record.suggestions" :key="`${record.id}-${index}`">{{ item }}</li>
            </ul>
            <p v-else>建议：暂无</p>
          </div>
        </details>
      </article>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { knowledgeAPI, trainingAPI } from '../api'
import type { Knowledge, TrainingRecord } from '../types'
import { formatDateTime } from '../utils/date'

const loading = ref(false)
const errorMessage = ref('')
const selectedKnowledgeId = ref(0)
const knowledgeList = ref<Knowledge[]>([])
const records = ref<TrainingRecord[]>([])

const knowledgeTitleMap = computed(() =>
  knowledgeList.value.reduce<Record<number, string>>((acc, item) => {
    acc[item.id] = item.title
    return acc
  }, {})
)

const filteredRecords = computed(() => {
  if (selectedKnowledgeId.value === 0) {
    return records.value
  }
  return records.value.filter((item) => item.knowledgeId === selectedKnowledgeId.value)
})

async function refresh() {
  loading.value = true
  errorMessage.value = ''
  try {
    const [knowledge, history] = await Promise.all([
      knowledgeAPI.getList(),
      trainingAPI.getAllHistory(),
    ])
    knowledgeList.value = knowledge
    records.value = history
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '加载历史失败'
  } finally {
    loading.value = false
  }
}

onMounted(refresh)
</script>

<style scoped>
.history-page {
  display: grid;
  gap: 16px;
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
  padding: 12px 16px;
  display: flex;
  justify-content: space-between;
  align-items: end;
  gap: 12px;
}

label {
  display: grid;
  gap: 6px;
}

label span {
  font-size: 14px;
  color: #475569;
}

select {
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  padding: 8px 10px;
  min-width: 260px;
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
  gap: 10px;
}

.history-card {
  padding: 14px;
}

.history-card header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}

.history-card h2 {
  margin: 0;
  font-size: 18px;
}

.overall {
  color: #1d4ed8;
  font-weight: 700;
}

.meta {
  color: #64748b;
  font-size: 13px;
  margin: 6px 0 8px;
}

.question,
.answer {
  margin: 4px 0;
  color: #334155;
}

details {
  margin-top: 8px;
}

summary {
  cursor: pointer;
  color: #1d4ed8;
}

.detail {
  margin-top: 8px;
  display: grid;
  gap: 4px;
}

.detail p {
  margin: 0;
}

.detail ul {
  margin: 0;
  padding-left: 18px;
}

@media (max-width: 768px) {
  .filter-card {
    flex-direction: column;
    align-items: flex-start;
  }

  select {
    min-width: 220px;
  }
}
</style>
