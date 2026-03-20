<template>
  <section class="training-page">
    <header class="page-header">
      <div>
        <div class="header-badge">训练进行中</div>
        <h1>{{ session?.knowledgeTitle || '训练会话' }}</h1>
      </div>
      <div v-if="session" class="session-meta">
        <span class="pill pill-blue">{{ questionTypeLabelMap[session.questionType] }}</span>
        <span class="pill pill-blue">{{ difficultyLabelMap[session.difficulty] }}</span>
        <span class="progress-indicator">
          <span class="progress-bar">
            <span class="progress-fill" :style="{ width: `${(session.sequence.current / session.sequence.total) * 100}%` }"></span>
          </span>
          <span class="progress-text">{{ session.sequence.current }} / {{ session.sequence.total }}</span>
        </span>
      </div>
    </header>

    <AppStateCard v-if="loading" variant="loading" message="正在加载训练会话..." />
    <AppStateCard v-else-if="errorMessage" variant="error" :message="errorMessage" />

    <div v-else-if="session" class="training-layout">
      <section class="card question-card">
        <div class="card-head">
          <h2>📝 问题</h2>
          <div class="hint-actions">
            <span v-if="session.hintAvailable" class="hint-badge available">💡 提示可用</span>
            <span v-else-if="hint" class="hint-badge used">已获取提示</span>
            <button
              v-if="session.hintAvailable"
              class="btn btn-hint"
              type="button"
              :disabled="hintLoading || submitting"
              @click="loadHint"
            >
              {{ hintLoading ? '提示生成中...' : '获取提示' }}
            </button>
          </div>
        </div>
        <p class="question-text">{{ session.question }}</p>
        <div v-if="hint" class="hint-panel animate-slideUp">
          <span class="hint-label">💡 答题提示</span>
          <p>{{ hint }}</p>
        </div>
      </section>

      <section class="card answer-card">
        <div class="card-head">
          <h2>✍️ 你的回答</h2>
          <span class="char-count" :class="{ warn: answer.length > 2000 }">{{ answer.length }} 字</span>
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
        <p v-if="submitting" class="submit-note animate-pulse">
          {{ isLastQuestion ? '系统正在评分并整理最终反馈...' : '系统正在评估本题并生成下一题...' }}
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
  gap: var(--sp-5);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: var(--sp-4);
}

.header-badge {
  display: inline-flex;
  align-items: center;
  padding: 5px 12px;
  border-radius: var(--radius-full);
  background: linear-gradient(135deg, rgba(16, 185, 129, 0.1), rgba(6, 182, 212, 0.1));
  border: 1px solid rgba(16, 185, 129, 0.2);
  color: var(--clr-success);
  font-size: var(--fs-xs);
  font-weight: 700;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  margin-bottom: var(--sp-2);
}

.page-header h1 {
  margin: 0;
  font-size: var(--fs-3xl);
  font-weight: 800;
  letter-spacing: -0.02em;
}

.session-meta {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: var(--sp-2);
  align-items: center;
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

.progress-indicator {
  display: flex;
  align-items: center;
  gap: var(--sp-2);
}

.progress-bar {
  width: 80px;
  height: 6px;
  background: var(--clr-bg-secondary);
  border-radius: var(--radius-full);
  overflow: hidden;
}

.progress-fill {
  display: block;
  height: 100%;
  background: linear-gradient(90deg, var(--clr-primary), var(--clr-accent));
  border-radius: var(--radius-full);
  transition: width var(--duration-slow) var(--ease-out);
}

.progress-text {
  font-size: var(--fs-xs);
  font-weight: 700;
  color: var(--clr-text-secondary);
}

.training-layout {
  display: grid;
  gap: var(--sp-4);
}

.question-card,
.answer-card {
  padding: var(--sp-5);
}

.card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--sp-2);
}

.hint-actions {
  display: flex;
  align-items: center;
  gap: var(--sp-2);
  flex-wrap: wrap;
}

.card-head h2 {
  margin: 0;
  font-size: var(--fs-xl);
  font-weight: 700;
}

.char-count {
  font-size: var(--fs-sm);
  color: var(--clr-text-tertiary);
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

.char-count.warn {
  color: var(--clr-warning);
}

.hint-badge {
  display: inline-flex;
  align-items: center;
  padding: 5px 12px;
  border-radius: var(--radius-full);
  font-size: var(--fs-xs);
  font-weight: 700;
}

.hint-badge.available {
  background: var(--clr-warning-bg);
  color: #92400e;
  border: 1px solid var(--clr-warning-border);
}

.hint-badge.used {
  background: var(--clr-bg-secondary);
  color: var(--clr-text-secondary);
}

.btn-hint {
  border-color: #fdba74;
  color: #9a3412;
  background: #fff7ed;
}

.btn-hint:hover {
  background: #fef3c7;
}

.question-text {
  margin: var(--sp-4) 0 0;
  font-size: var(--fs-lg);
  line-height: 1.8;
  color: var(--clr-text);
  white-space: pre-wrap;
}

.hint-panel {
  margin-top: var(--sp-4);
  padding: var(--sp-4);
  border-radius: var(--radius-md);
  background: linear-gradient(135deg, #fff7ed, #fffbeb);
  border: 1px solid #fdba74;
  display: grid;
  gap: var(--sp-2);
}

.hint-label {
  font-size: var(--fs-xs);
  letter-spacing: 0.1em;
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

textarea {
  width: 100%;
  margin-top: var(--sp-4);
  border: 1.5px solid var(--clr-border);
  border-radius: var(--radius-md);
  padding: var(--sp-4);
  resize: vertical;
  min-height: 280px;
  font-size: var(--fs-sm);
  line-height: 1.7;
  transition: border-color var(--duration-fast) var(--ease-out), box-shadow var(--duration-fast) var(--ease-out);
}

textarea:focus {
  outline: none;
  border-color: var(--clr-primary);
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.1);
}

.footer-actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--sp-2);
  margin-top: var(--sp-4);
}

.submit-note {
  margin: var(--sp-3) 0 0;
  font-size: var(--fs-sm);
  color: var(--clr-text-tertiary);
  line-height: 1.6;
  text-align: right;
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
  }
  .session-meta {
    justify-content: flex-start;
  }
  .card-head {
    flex-direction: column;
    align-items: flex-start;
  }
  .footer-actions {
    flex-direction: column;
  }
  .footer-actions .btn {
    width: 100%;
  }
}
</style>
