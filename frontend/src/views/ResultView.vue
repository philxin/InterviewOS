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

    <AppStateCard v-if="loading" variant="loading" message="正在加载结果详情..." />
    <AppStateCard v-else-if="errorMessage" variant="error" :message="errorMessage" />

    <div v-else-if="detail && answeredQuestions.length > 0" class="result-grid">
      <div v-if="refreshing" class="sync-banner">结果已展示，正在同步服务端最终详情...</div>
      <section class="top-grid">
        <TrainingFeedbackBand v-if="sessionBand" :band="sessionBand" />
        <div v-else class="card summary-card">
          <span class="summary-label">反馈档位</span>
          <h2>暂无档位</h2>
        </div>
        <div class="card summary-card">
          <span class="summary-label">主要问题</span>
          <h2>{{ detail.majorIssueSummary || '暂无主要问题摘要' }}</h2>
          <div class="summary-meta">
            <span>会话得分 {{ summaryScore }}</span>
            <span>已完成 {{ detail.answeredCount }} / {{ detail.questionCount }} 题</span>
          </div>
        </div>
      </section>

      <section v-for="question in answeredQuestions" :key="question.questionId" class="card detail-card question-card">
        <div class="question-head">
          <h2>
            第 {{ question.orderNo }} 题 ·
            {{ questionTypeLabelMap[question.questionType] }} ·
            {{ difficultyLabelMap[question.difficulty] }}
          </h2>
          <span class="score-pill">得分 {{ question.feedback.score }}</span>
        </div>
        <p class="question-title">{{ question.question }}</p>
        <p class="answer-label">你的回答</p>
        <p class="answer-content">{{ question.answer || '本次未提交回答。' }}</p>

        <div class="feedback-grid">
          <section class="feedback-block">
            <h3>主要问题</h3>
            <p>{{ question.feedback.majorIssue || '暂无主要问题。' }}</p>
          </section>

          <section class="feedback-block">
            <h3>缺失点</h3>
            <ul v-if="question.feedback.missingPoints.length > 0">
              <li v-for="(item, index) in question.feedback.missingPoints" :key="`missing-${question.questionId}-${index}`">
                {{ item }}
              </li>
            </ul>
            <p v-else>暂无缺失点建议。</p>
          </section>

          <section class="feedback-block">
            <h3>更好回答思路</h3>
            <ul v-if="question.feedback.betterAnswerApproach.length > 0">
              <li
                v-for="(item, index) in question.feedback.betterAnswerApproach"
                :key="`approach-${question.questionId}-${index}`"
              >
                {{ item }}
              </li>
            </ul>
            <p v-else>暂无思路建议。</p>
          </section>

          <section class="feedback-block">
            <h3>自然参考表达</h3>
            <p>{{ question.feedback.naturalExampleAnswer || '暂无参考表达。' }}</p>
          </section>

          <section class="feedback-block">
            <h3>薄弱标签</h3>
            <div v-if="question.feedback.weakTags.length > 0" class="tag-list">
              <span v-for="tag in question.feedback.weakTags" :key="`${question.questionId}-${tag}`" class="tag-chip">
                #{{ tag }}
              </span>
            </div>
            <p v-else>暂无标签建议。</p>
          </section>

          <section class="feedback-block">
            <h3>掌握度变化</h3>
            <p>{{ question.feedback.masteryBefore }} → {{ question.feedback.masteryAfter }}</p>
          </section>
        </div>
      </section>
    </div>

    <AppStateCard v-else variant="empty" message="暂无结果，请先完成一次训练。">
      <template #actions>
        <button class="btn btn-primary" type="button" @click="goHome">返回知识点</button>
      </template>
    </AppStateCard>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppStateCard from '../components/AppStateCard.vue'
import TrainingFeedbackBand from '../components/TrainingFeedbackBand.vue'
import { trainingAPI } from '../api'
import { useTrainingStore } from '../stores/training'
import type { TrainingSessionDetail } from '../types'

const route = useRoute()
const router = useRouter()
const trainingStore = useTrainingStore()

const loading = ref(false)
const refreshing = ref(false)
const errorMessage = ref('')
const detail = ref<TrainingSessionDetail | null>(trainingStore.latestDetail)

const sessionId = computed(() => String(route.params.sessionId || ''))
const questionTypeLabelMap: Record<string, string> = {
  FUNDAMENTAL: '基础题',
  PROJECT: '项目题',
  SCENARIO: '场景题',
}
const difficultyLabelMap: Record<string, string> = {
  EASY: '简单',
  MEDIUM: '中等',
  HARD: '困难',
}

type QuestionDetail = TrainingSessionDetail['questions'][number]
type AnsweredQuestionDetail = QuestionDetail & {
  feedback: NonNullable<QuestionDetail['feedback']>
}

function isAnsweredQuestion(question: QuestionDetail): question is AnsweredQuestionDetail {
  return question.feedback !== null
}

const answeredQuestions = computed<AnsweredQuestionDetail[]>(() => {
  if (!detail.value) {
    return []
  }
  return detail.value.questions.filter(isAnsweredQuestion)
})

const sessionBand = computed(() => {
  if (detail.value?.band) {
    return detail.value.band
  }
  return answeredQuestions.value[0]?.feedback.band ?? null
})

const summaryScore = computed(() => {
  if (detail.value?.sessionScore !== null && detail.value?.sessionScore !== undefined) {
    return detail.value.sessionScore
  }
  if (answeredQuestions.value.length === 0) {
    return 0
  }
  const average = answeredQuestions.value
    .map((question) => question.feedback.score)
    .reduce((total, score) => total + score, 0) / answeredQuestions.value.length
  return Math.round(average)
})

async function fetchDetail(showBlockingState: boolean) {
  if (showBlockingState) {
    loading.value = true
  } else {
    refreshing.value = true
  }
  try {
    const nextDetail = await trainingAPI.getSessionDetail(sessionId.value)
    detail.value = nextDetail
    trainingStore.setLatestDetail(nextDetail)
  } catch (error) {
    if (!detail.value || detail.value.sessionId !== sessionId.value) {
      errorMessage.value = error instanceof Error ? error.message : '加载结果失败'
    }
  } finally {
    loading.value = false
    refreshing.value = false
  }
}

async function loadDetail() {
  if (!sessionId.value) {
    errorMessage.value = '缺少 sessionId'
    return
  }

  if (detail.value?.sessionId === sessionId.value) {
    errorMessage.value = ''
    void fetchDetail(false)
    return
  }

  errorMessage.value = ''
  await fetchDetail(true)
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

.result-grid {
  display: grid;
  gap: 14px;
}

.sync-banner {
  padding: 12px 14px;
  border-radius: 14px;
  background: #ecfeff;
  border: 1px solid #67e8f9;
  color: #155e75;
  font-size: 14px;
  line-height: 1.6;
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

.question-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 10px;
}

.question-head h2 {
  font-size: 20px;
}

.score-pill {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  border-radius: 999px;
  padding: 4px 10px;
  background: #ecfeff;
  border: 1px solid #67e8f9;
  color: #0e7490;
  font-size: 12px;
  font-weight: 700;
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

.feedback-grid {
  margin-top: 18px;
  display: grid;
  gap: 12px;
}

.feedback-block {
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 12px;
  background: #f8fafc;
}

.feedback-block h3 {
  margin: 0;
  font-size: 14px;
  color: #334155;
}

.feedback-block p {
  margin: 10px 0 0;
  color: #334155;
  line-height: 1.7;
  white-space: pre-wrap;
}

.feedback-block ul {
  margin: 10px 0 0;
  padding-left: 18px;
  display: grid;
  gap: 8px;
  color: #334155;
}

.detail-card ul {
  margin: 16px 0 0;
  padding-left: 18px;
  display: grid;
  gap: 8px;
  color: #334155;
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
