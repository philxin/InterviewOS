<template>
  <section class="result-page">
    <header class="page-header">
      <div>
        <p class="eyebrow">训练结果</p>
        <h1>{{ detail?.knowledgeTitle || '结果详情' }}</h1>
      </div>
      <div class="header-actions">
        <button class="btn" type="button" @click="goHistory">历史</button>
        <button class="btn" type="button" @click="goHome">知识点</button>
        <button
          v-if="detail?.knowledgeId"
          class="btn btn-primary"
          type="button"
          @click="retryTraining(detail.knowledgeId)"
        >
          再训练一次
        </button>
      </div>
    </header>

    <div v-if="loading" class="card state-card">正在加载结果详情...</div>
    <div v-else-if="errorMessage" class="card state-card error">{{ errorMessage }}</div>

    <div v-else-if="detail && primaryQuestion && primaryFeedback" class="result-grid">
      <section class="top-grid">
        <TrainingFeedbackBand :band="primaryFeedback.band" />
        <div class="card summary-card">
          <span class="summary-label">主要问题</span>
          <h2>{{ primaryFeedback.majorIssue || detail.majorIssueSummary || '暂无主要问题摘要' }}</h2>
          <div class="summary-meta">
            <span>得分 {{ primaryFeedback.score }}</span>
            <span>掌握度 {{ primaryFeedback.masteryBefore }} → {{ primaryFeedback.masteryAfter }}</span>
          </div>
        </div>
      </section>

      <section class="card detail-card">
        <h2>问题与回答</h2>
        <p class="question-title">{{ primaryQuestion.question }}</p>
        <p class="answer-label">你的回答</p>
        <p class="answer-content">{{ primaryQuestion.answer || '本次未提交回答。' }}</p>
      </section>

      <section class="card detail-card">
        <h2>缺失点</h2>
        <ul>
          <li v-for="(item, index) in primaryFeedback.missingPoints" :key="`missing-${index}`">{{ item }}</li>
        </ul>
      </section>

      <section class="card detail-card">
        <h2>更好回答思路</h2>
        <ul>
          <li v-for="(item, index) in primaryFeedback.betterAnswerApproach" :key="`approach-${index}`">
            {{ item }}
          </li>
        </ul>
      </section>

      <section class="card detail-card example-card">
        <h2>自然参考表达</h2>
        <p>{{ primaryFeedback.naturalExampleAnswer || '暂无参考表达。' }}</p>
      </section>

      <section class="card detail-card">
        <h2>薄弱标签</h2>
        <div v-if="primaryFeedback.weakTags.length > 0" class="tag-list">
          <span v-for="tag in primaryFeedback.weakTags" :key="tag" class="tag-chip">#{{ tag }}</span>
        </div>
        <p v-else>暂无标签建议。</p>
      </section>
    </div>

    <div v-else class="card state-card">
      <p>暂无结果，请先完成一次训练。</p>
      <button class="btn btn-primary" type="button" @click="goHome">返回知识点</button>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import TrainingFeedbackBand from '../components/TrainingFeedbackBand.vue'
import { trainingAPI } from '../api'
import { useTrainingStore } from '../stores/training'
import type { TrainingSessionDetail } from '../types'

const route = useRoute()
const router = useRouter()
const trainingStore = useTrainingStore()

const loading = ref(false)
const errorMessage = ref('')
const detail = ref<TrainingSessionDetail | null>(trainingStore.latestDetail)

const sessionId = computed(() => String(route.params.sessionId || ''))
const primaryQuestion = computed(() => detail.value?.questions[0] ?? null)
const primaryFeedback = computed(() => primaryQuestion.value?.feedback ?? null)

async function loadDetail() {
  if (!sessionId.value) {
    errorMessage.value = '缺少 sessionId'
    return
  }

  if (detail.value?.sessionId === sessionId.value) {
    return
  }

  loading.value = true
  errorMessage.value = ''
  try {
    detail.value = await trainingAPI.getSessionDetail(sessionId.value)
    trainingStore.setLatestDetail(detail.value)
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '加载结果失败'
  } finally {
    loading.value = false
  }
}

function goHome() {
  router.push('/')
}

function goHistory() {
  router.push('/history')
}

function retryTraining(knowledgeId: number) {
  router.push(`/training/${knowledgeId}`)
}

onMounted(loadDetail)
</script>

<style scoped>
.result-page {
  display: grid;
  gap: 16px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 14px;
}

.eyebrow {
  margin: 0 0 8px;
  font-size: 12px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: #7c2d12;
  font-weight: 700;
}

.page-header h1 {
  margin: 0;
  font-size: 32px;
}

.header-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.state-card {
  padding: 20px;
}

.error {
  color: #b91c1c;
  border-color: #fecaca;
  background: #fff1f2;
}

.result-grid {
  display: grid;
  gap: 14px;
}

.top-grid {
  display: grid;
  grid-template-columns: minmax(260px, 360px) 1fr;
  gap: 14px;
}

.summary-card,
.detail-card {
  padding: 18px;
}

.summary-label {
  display: inline-flex;
  margin-bottom: 10px;
  font-size: 12px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: #475569;
  font-weight: 700;
}

.summary-card h2,
.detail-card h2 {
  margin: 0;
  font-size: 22px;
  line-height: 1.4;
}

.summary-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 14px;
  color: #475569;
  font-size: 14px;
}

.question-title {
  margin: 16px 0 0;
  font-size: 18px;
  color: #1f2937;
  line-height: 1.7;
}

.answer-label {
  margin: 18px 0 6px;
  font-size: 13px;
  font-weight: 700;
  color: #64748b;
}

.answer-content {
  margin: 0;
  color: #334155;
  white-space: pre-wrap;
  line-height: 1.8;
}

.detail-card ul {
  margin: 16px 0 0;
  padding-left: 18px;
  display: grid;
  gap: 8px;
  color: #334155;
}

.example-card p,
.detail-card > p:last-child {
  margin: 16px 0 0;
  color: #334155;
  line-height: 1.8;
  white-space: pre-wrap;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 16px;
}

.tag-chip {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 4px 10px;
  border-radius: 999px;
  background: #eff6ff;
  color: #1d4ed8;
  font-size: 12px;
  font-weight: 700;
}

@media (max-width: 900px) {
  .page-header,
  .top-grid {
    grid-template-columns: 1fr;
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
