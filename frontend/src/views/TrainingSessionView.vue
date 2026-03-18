<template>
  <section class="training-page">
    <header class="page-header">
      <div>
        <p class="eyebrow">训练进行中</p>
        <h1>{{ session?.knowledgeTitle || '训练会话' }}</h1>
      </div>
      <div v-if="session" class="session-meta">
        <span class="pill">{{ questionTypeLabelMap[session.questionType] }}</span>
        <span class="pill">{{ difficultyLabelMap[session.difficulty] }}</span>
        <span class="sequence">第 {{ session.sequence.current }} / {{ session.sequence.total }} 题</span>
      </div>
    </header>

    <AppStateCard v-if="loading" variant="loading" message="正在加载训练会话..." />
    <AppStateCard v-else-if="errorMessage" variant="error" :message="errorMessage" />

    <div v-else-if="session" class="training-layout">
      <section class="card question-card">
        <div class="card-head">
          <h2>问题</h2>
          <div class="hint-actions">
            <span v-if="session.hintAvailable" class="hint-badge">提示能力已开启</span>
            <span v-else-if="hint" class="hint-badge used">提示已获取</span>
            <button
              v-if="session.hintAvailable"
              class="btn btn-secondary"
              type="button"
              :disabled="hintLoading || submitting"
              @click="loadHint"
            >
              {{ hintLoading ? '提示生成中...' : '获取提示' }}
            </button>
          </div>
        </div>
        <p class="question-text">{{ session.question }}</p>
        <div v-if="hint" class="hint-panel">
          <span class="hint-label">答题提示</span>
          <p>{{ hint }}</p>
        </div>
      </section>

      <section class="card answer-card">
        <div class="card-head">
          <h2>你的回答</h2>
          <span class="helper">{{ answer.length }} 字</span>
        </div>
        <textarea
          v-model.trim="answer"
          rows="14"
          placeholder="先给结论，再补关键原理、条件和项目例子。"
        />
        <div class="footer-actions">
          <button class="btn" type="button" :disabled="submitting" @click="goBack">取消</button>
          <button class="btn btn-primary" type="button" :disabled="submitting || !answer" @click="submitAnswer">
            {{ submitting ? '提交中...' : isLastQuestion ? '提交并查看反馈' : '提交并进入下一题' }}
          </button>
        </div>
        <p v-if="submitting" class="submit-note">
          {{ isLastQuestion ? '系统正在评分并整理最终反馈。' : '系统正在评估本题并生成下一题。' }}
        </p>
      </section>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { trainingAPI } from '../api'
import AppStateCard from '../components/AppStateCard.vue'
import { useTrainingStore } from '../stores/training'
import type { TrainingSessionDetail, TrainingSessionStartResponse } from '../types'

const route = useRoute()
const router = useRouter()
const trainingStore = useTrainingStore()

const loading = ref(false)
const submitting = ref(false)
const hintLoading = ref(false)
const errorMessage = ref('')
const answer = ref(trainingStore.currentAnswer || '')
const hint = ref(trainingStore.currentHint || '')
const session = ref<TrainingSessionStartResponse | null>(trainingStore.currentSession)

const sessionId = computed(() => String(route.params.sessionId || ''))
const isLastQuestion = computed(() => {
  if (!session.value) {
    return false
  }
  return session.value.sequence.current >= session.value.sequence.total
})

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

async function initSession() {
  if (
    session.value &&
    session.value.sessionId === sessionId.value &&
    trainingStore.latestDetail?.sessionId === sessionId.value
  ) {
    trainingStore.setCurrentAnswer(answer.value)
    trainingStore.setCurrentHint(hint.value)
    return
  }

  try {
    const detail = await trainingAPI.getSessionDetail(sessionId.value)
    if (detail.completedAt) {
      await router.replace(`/result/${detail.sessionId}`)
      return
    }
    hydrateSessionFromDetail(detail)
    trainingStore.setLatestDetail(detail)
    trainingStore.setCurrentAnswer(answer.value)
    trainingStore.setCurrentHint(hint.value)
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '加载训练会话失败'
  }
}

function hydrateSessionFromDetail(detail: TrainingSessionDetail) {
  const fallbackQuestion = detail.questions.length > 0 ? detail.questions[detail.questions.length - 1] : null
  const activeQuestion = detail.questions.find((question) => !question.answer) ?? fallbackQuestion
  if (!activeQuestion) {
    errorMessage.value = '训练会话不存在题目'
    return
  }
  session.value = {
    sessionId: detail.sessionId,
    questionId: activeQuestion.questionId,
    knowledgeId: detail.knowledgeId,
    knowledgeTitle: detail.knowledgeTitle,
    question: activeQuestion.question,
    questionType: activeQuestion.questionType,
    difficulty: activeQuestion.difficulty,
    hintAvailable: activeQuestion.hintAvailable,
    sequence: {
      current: activeQuestion.orderNo,
      total: detail.questionCount,
    },
  }
  answer.value = activeQuestion.answer || ''
  hint.value = activeQuestion.hintText || ''
  trainingStore.setCurrentSession(session.value)
  trainingStore.setCurrentAnswer(answer.value)
  trainingStore.setCurrentHint(hint.value)
}

function syncDetailHint(nextHint: string) {
  if (!session.value || !trainingStore.latestDetail || trainingStore.latestDetail.sessionId !== sessionId.value) {
    return
  }
  const targetQuestion = trainingStore.latestDetail.questions.find(
    (question) => question.questionId === session.value?.questionId,
  )
  if (!targetQuestion) {
    return
  }
  targetQuestion.hintText = nextHint
  targetQuestion.hintUsed = true
  targetQuestion.hintAvailable = false
}

async function loadHint() {
  if (!session.value) {
    errorMessage.value = '训练会话不存在'
    return
  }

  hintLoading.value = true
  errorMessage.value = ''
  try {
    const response = await trainingAPI.getHint(session.value.sessionId, session.value.questionId)
    hint.value = response.hint
    session.value = {
      ...session.value,
      hintAvailable: false,
    }
    trainingStore.setCurrentSession(session.value)
    trainingStore.setCurrentHint(response.hint)
    syncDetailHint(response.hint)
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '获取提示失败'
  } finally {
    hintLoading.value = false
  }
}

async function submitAnswer() {
  if (!session.value) {
    errorMessage.value = '训练会话不存在'
    return
  }
  if (!answer.value) {
    errorMessage.value = '回答不能为空'
    return
  }

  submitting.value = true
  errorMessage.value = ''
  try {
    trainingStore.setCurrentAnswer(answer.value)
    trainingStore.setCurrentHint(hint.value)
    const feedback = await trainingAPI.submitAnswer(session.value.sessionId, {
      questionId: session.value.questionId,
      answer: answer.value,
    })
    const detail = await trainingAPI.getSessionDetail(session.value.sessionId)
    trainingStore.setLatestDetail(detail)

    if (detail.completedAt || detail.answeredCount >= detail.questionCount) {
      trainingStore.setLatestFeedback(feedback)
      trainingStore.setCurrentAnswer('')
      trainingStore.setCurrentHint('')
      await router.push(`/result/${session.value.sessionId}`)
      return
    }

    trainingStore.setLatestFeedback(null)
    hydrateSessionFromDetail(detail)
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '提交失败'
  } finally {
    submitting.value = false
  }
}

function goBack() {
  router.push('/')
}

onMounted(async () => {
  loading.value = true
  try {
    await initSession()
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.training-page {
  display: grid;
  gap: 16px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.eyebrow {
  margin: 0 0 8px;
  font-size: 12px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: #0f766e;
  font-weight: 700;
}

.page-header h1 {
  margin: 0;
  font-size: 32px;
}

.session-meta {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.pill,
.sequence,
.hint-badge {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.pill,
.sequence {
  background: #eff6ff;
  color: #1d4ed8;
}

.hint-badge {
  background: #fef3c7;
  color: #92400e;
}

.hint-badge.used {
  background: #e2e8f0;
  color: #334155;
}

.training-layout {
  display: grid;
  gap: 14px;
}

.question-card,
.answer-card {
  padding: 18px;
}

.card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}

.hint-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.card-head h2 {
  margin: 0;
  font-size: 20px;
}

.helper {
  color: #64748b;
  font-size: 13px;
}

.question-text {
  margin: 16px 0 0;
  font-size: 18px;
  line-height: 1.8;
  color: #1f2937;
  white-space: pre-wrap;
}

.hint-panel {
  margin-top: 16px;
  padding: 14px 16px;
  border-radius: 14px;
  background: #fff7ed;
  border: 1px solid #fdba74;
  display: grid;
  gap: 8px;
}

.hint-label {
  font-size: 12px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: #9a3412;
  font-weight: 700;
}

.hint-panel p {
  margin: 0;
  color: #7c2d12;
  line-height: 1.7;
  white-space: pre-wrap;
}

.btn-secondary {
  border-color: #fdba74;
  color: #9a3412;
  background: #fff7ed;
}

textarea {
  width: 100%;
  margin-top: 16px;
  border: 1px solid #cbd5e1;
  border-radius: 14px;
  padding: 14px;
  resize: vertical;
  min-height: 280px;
}

.footer-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 16px;
}

.submit-note {
  margin: 12px 0 0;
  font-size: 13px;
  color: #64748b;
  line-height: 1.6;
}

@media (max-width: 768px) {
  .page-header,
  .card-head,
  .footer-actions,
  .hint-actions {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
