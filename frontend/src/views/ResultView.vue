<template>
  <section class="result-page">
    <header class="page-header">
      <div>
        <div class="header-badge">训练结果</div>
        <h1>{{ detail?.knowledgeTitle || '结果详情' }}</h1>
      </div>
      <div class="header-actions">
        <button class="btn" type="button" @click="goHistory">📊 历史</button>
        <button class="btn" type="button" @click="goHome">📋 知识点</button>
        <button
          v-if="detail?.knowledgeId"
          class="btn btn-primary"
          type="button"
          @click="retryTraining(detail.knowledgeId)"
        >
          🔄 再训练一次
        </button>
      </div>
    </header>

    <AppStateCard v-if="loading" variant="loading" message="正在加载结果详情..." />
    <AppStateCard v-else-if="errorMessage" variant="error" :message="errorMessage" />

    <div v-else-if="detail && answeredQuestions.length > 0" class="result-grid">
      <div v-if="refreshing" class="sync-banner animate-pulse">
        ⏳ 结果已展示，正在同步服务端最终详情...
      </div>

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
            <span class="pill pill-dark">会话得分 {{ summaryScore }}</span>
            <span class="meta-text">已完成 {{ detail.answeredCount }} / {{ detail.questionCount }} 题</span>
          </div>
        </div>
      </section>

      <section v-for="question in answeredQuestions" :key="question.questionId" class="card question-card animate-slideUp">
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
            <h3>🔍 主要问题</h3>
            <p>{{ question.feedback.majorIssue || '暂无主要问题。' }}</p>
          </section>

          <section class="feedback-block">
            <h3>📋 缺失点</h3>
            <ul v-if="question.feedback.missingPoints.length > 0">
              <li v-for="(item, index) in question.feedback.missingPoints" :key="`missing-${question.questionId}-${index}`">
                {{ item }}
              </li>
            </ul>
            <p v-else>暂无缺失点建议。</p>
          </section>

          <section class="feedback-block">
            <h3>💡 更好回答思路</h3>
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
            <h3>✍️ 自然参考表达</h3>
            <p>{{ question.feedback.naturalExampleAnswer || '暂无参考表达。' }}</p>
          </section>

          <section class="feedback-block">
            <h3>🏷️ 薄弱标签</h3>
            <div v-if="question.feedback.weakTags.length > 0" class="tag-list">
              <span v-for="tag in question.feedback.weakTags" :key="`${question.questionId}-${tag}`" class="tag-chip">
                #{{ tag }}
              </span>
            </div>
            <p v-else>暂无标签建议。</p>
          </section>

          <section class="feedback-block">
            <h3>📈 掌握度变化</h3>
            <div class="mastery-change">
              <span class="mastery-from">{{ question.feedback.masteryBefore }}%</span>
              <span class="mastery-arrow">→</span>
              <span class="mastery-to">{{ question.feedback.masteryAfter }}%</span>
            </div>
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

function goHome() { router.push('/') }
function goHistory() { router.push('/history') }
function retryTraining(knowledgeId: number) { router.push(`/training/${knowledgeId}`) }

onMounted(loadDetail)
</script>

<style scoped>
.result-page {
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
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.1), rgba(236, 72, 153, 0.1));
  border: 1px solid rgba(99, 102, 241, 0.2);
  color: var(--clr-primary);
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

.header-actions {
  display: flex;
  gap: var(--sp-2);
  flex-wrap: wrap;
}

.result-grid {
  display: grid;
  gap: var(--sp-4);
}

.sync-banner {
  padding: var(--sp-3) var(--sp-4);
  border-radius: var(--radius-md);
  background: linear-gradient(135deg, rgba(6, 182, 212, 0.08), rgba(99, 102, 241, 0.08));
  border: 1px solid rgba(6, 182, 212, 0.2);
  color: var(--clr-accent-dark);
  font-size: var(--fs-sm);
  font-weight: 600;
}

.top-grid {
  display: grid;
  grid-template-columns: minmax(260px, 360px) 1fr;
  gap: var(--sp-4);
}

.summary-card {
  padding: var(--sp-5);
}

.summary-label {
  display: inline-flex;
  margin-bottom: var(--sp-2);
  font-size: var(--fs-xs);
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: var(--clr-text-secondary);
  font-weight: 700;
}

.summary-card h2 {
  margin: 0;
  font-size: var(--fs-xl);
  line-height: 1.4;
  font-weight: 700;
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

.pill-dark {
  background: var(--clr-text);
  color: var(--clr-text-inverse);
}

.summary-meta {
  display: flex;
  flex-wrap: wrap;
  gap: var(--sp-3);
  margin-top: var(--sp-3);
  align-items: center;
}

.question-card {
  padding: var(--sp-5);
}

.question-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: var(--sp-3);
}

.question-head h2 {
  margin: 0;
  font-size: var(--fs-xl);
  font-weight: 700;
}

.score-pill {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  border-radius: var(--radius-full);
  padding: 4px 14px;
  background: linear-gradient(135deg, var(--clr-primary-50), rgba(6, 182, 212, 0.08));
  border: 1px solid rgba(99, 102, 241, 0.2);
  color: var(--clr-primary-dark);
  font-size: var(--fs-sm);
  font-weight: 700;
}

.question-title {
  margin: var(--sp-4) 0 0;
  font-size: var(--fs-lg);
  color: var(--clr-text);
  line-height: 1.7;
}

.answer-label {
  margin: var(--sp-5) 0 var(--sp-1);
  font-size: var(--fs-sm);
  font-weight: 700;
  color: var(--clr-text-tertiary);
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.answer-content {
  margin: 0;
  color: var(--clr-text-secondary);
  white-space: pre-wrap;
  line-height: 1.8;
  font-size: var(--fs-sm);
  padding: var(--sp-4);
  background: var(--clr-bg-secondary);
  border-radius: var(--radius-md);
}

.feedback-grid {
  margin-top: var(--sp-5);
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--sp-3);
}

.feedback-block {
  border: 1px solid var(--clr-border);
  border-radius: var(--radius-md);
  padding: var(--sp-4);
  background: var(--clr-surface);
  transition: box-shadow var(--duration-fast) var(--ease-out);
}

.feedback-block:hover {
  box-shadow: var(--shadow-sm);
}

.feedback-block h3 {
  margin: 0;
  font-size: var(--fs-sm);
  font-weight: 700;
  color: var(--clr-text);
}

.feedback-block p {
  margin: var(--sp-2) 0 0;
  color: var(--clr-text-secondary);
  line-height: 1.7;
  white-space: pre-wrap;
  font-size: var(--fs-sm);
}

.feedback-block ul {
  margin: var(--sp-2) 0 0;
  padding-left: 18px;
  display: grid;
  gap: var(--sp-2);
  color: var(--clr-text-secondary);
  font-size: var(--fs-sm);
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: var(--sp-2);
  margin-top: var(--sp-3);
}

.tag-chip {
  display: inline-flex;
  align-items: center;
  min-height: 26px;
  padding: 3px 10px;
  border-radius: var(--radius-full);
  background: var(--clr-primary-50);
  color: var(--clr-primary-dark);
  font-size: var(--fs-xs);
  font-weight: 700;
}

.mastery-change {
  display: flex;
  align-items: center;
  gap: var(--sp-3);
  margin-top: var(--sp-2);
  font-size: var(--fs-2xl);
  font-weight: 800;
}

.mastery-from { color: var(--clr-text-tertiary); }
.mastery-arrow { color: var(--clr-text-tertiary); font-size: var(--fs-xl); }
.mastery-to { color: var(--clr-primary); }

.meta-text {
  color: var(--clr-text-tertiary);
  font-size: var(--fs-sm);
}

@media (max-width: 900px) {
  .page-header {
    flex-direction: column;
  }
  .top-grid {
    grid-template-columns: 1fr;
  }
  .feedback-grid {
    grid-template-columns: 1fr;
  }
}
</style>
