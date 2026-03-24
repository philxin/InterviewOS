<template>
  <section class="dashboard-page">
    <header class="page-header">
      <div class="header-content">
        <h1>知识点训练看板</h1>
        <p>概览你的训练进度，管理知识点和开始训练。</p>
      </div>
      <div class="header-actions">
        <button class="btn" type="button" @click="goInvitations">✉️ 邀请注册</button>
        <button class="btn" type="button" @click="goHistory">📊 训练历史</button>
        <button class="btn" type="button" @click="goImport">📥 批量导入</button>
        <button class="btn btn-primary" type="button" @click="goCreate">➕ 新建知识点</button>
      </div>
    </header>

    <AppStateCard v-if="loading" variant="loading" message="正在加载首页概览..." />
    <AppStateCard v-else-if="errorMessage" variant="error" :message="errorMessage" />
    <template v-else>
      <!-- Metrics -->
      <section class="overview-grid">
        <article class="metric-card metric-blue">
          <div class="metric-icon">🎯</div>
          <div class="metric-body">
            <span class="metric-label">近 7 天训练次数</span>
            <strong>{{ overview.progressSummary.trainedCountLast7Days }}</strong>
            <p>保持节奏比一次高分更重要。</p>
          </div>
        </article>
        <article class="metric-card metric-green">
          <div class="metric-icon">📈</div>
          <div class="metric-body">
            <span class="metric-label">近 7 天平均分</span>
            <strong>{{ overview.progressSummary.averageScoreLast7Days }}</strong>
            <p>先把平均稳定到 60+，再追求更高。</p>
          </div>
        </article>
        <article class="metric-card metric-purple">
          <div class="metric-icon">⚡</div>
          <div class="metric-body">
            <span class="metric-label">本周提升知识点</span>
            <strong>{{ overview.progressSummary.improvedKnowledgeCount }}</strong>
            <p>近 7 天掌握度提升的知识点数。</p>
          </div>
        </article>
      </section>

      <!-- Today Recommendations -->
      <section class="section-card recommend-section">
        <div class="section-head">
          <div>
            <h2>{{ recommendation.title || '今日推荐练习' }}</h2>
            <span class="section-tip">系统基于薄弱项和最近训练表现生成</span>
          </div>
        </div>
        <AppInlineState
          v-if="recommendationLoading"
          variant="loading"
          text="正在生成今日推荐训练包..."
        />
        <AppInlineState
          v-else-if="recommendationError"
          variant="error"
          :text="recommendationError"
        />
        <AppInlineState
          v-else-if="recommendation.items.length === 0"
          variant="empty"
          text="暂无推荐训练包，先补充更多知识点后再试。"
        />
        <div v-else class="item-list">
          <article
            v-for="item in recommendation.items"
            :key="`recommend-${item.knowledgeId}-${item.questionType}-${item.difficulty}`"
            class="list-item"
          >
            <div class="item-main">
              <strong>{{ resolveKnowledgeTitle(item.knowledgeId) }}</strong>
              <div class="item-meta">
                <span class="pill pill-blue">{{ questionTypeLabelMap[item.questionType] }}</span>
                <span class="pill pill-dark">{{ difficultyLabelMap[item.difficulty] }}</span>
              </div>
            </div>
            <button
              class="btn btn-primary"
              type="button"
              :disabled="startingKnowledgeId === item.knowledgeId"
              @click="startRecommendedTraining(item)"
            >
              {{ startingKnowledgeId === item.knowledgeId ? '创建中...' : '开始训练' }}
            </button>
          </article>
        </div>
      </section>

      <!-- Review Reminders -->
      <section class="section-card review-section">
        <div class="section-head">
          <div>
            <h2>回练提醒</h2>
            <span class="section-tip">结合掌握度、最近表现和训练间隔动态更新</span>
          </div>
        </div>
        <AppInlineState
          v-if="reviewReminderLoading"
          variant="loading"
          text="正在生成回练提醒..."
        />
        <AppInlineState
          v-else-if="reviewReminderError"
          variant="error"
          :text="reviewReminderError"
        />
        <AppInlineState
          v-else-if="reviewReminder.items.length === 0"
          variant="empty"
          text="暂无回练提醒，当前训练节奏良好。"
        />
        <div v-else class="item-list">
          <article
            v-for="item in reviewReminder.items"
            :key="`review-${item.knowledgeId}`"
            class="list-item review-item"
          >
            <div class="item-main">
              <strong>{{ item.knowledgeTitle }}</strong>
              <p class="item-reason">{{ item.reason }}</p>
              <div class="item-meta">
                <span class="pill pill-dark">权重 {{ item.reviewWeight }}</span>
                <span class="pill pill-blue">{{ questionTypeLabelMap[item.suggestedQuestionType] }}</span>
                <span class="pill pill-blue">{{ difficultyLabelMap[item.suggestedDifficulty] }}</span>
              </div>
              <p class="meta-text">
                上次训练：{{ item.lastTrainedAt ? formatDateTime(item.lastTrainedAt) : '暂无训练记录' }}
              </p>
            </div>
            <button
              class="btn btn-primary"
              type="button"
              :disabled="startingKnowledgeId === item.knowledgeId"
              @click="startReviewTraining(item)"
            >
              {{ startingKnowledgeId === item.knowledgeId ? '创建中...' : '立即回练' }}
            </button>
          </article>
        </div>
      </section>

      <!-- Weak + Recent -->
      <section class="summary-grid">
        <article class="section-card">
          <div class="section-head">
            <div>
              <h2>当前薄弱项</h2>
              <span class="section-tip">优先复练掌握度最低的知识点</span>
            </div>
          </div>
          <AppInlineState
            v-if="overview.weakKnowledgeItems.length === 0"
            variant="empty"
            text="暂无知识点数据，先创建 1-2 个训练主题。"
          />
          <div v-else class="weak-list">
            <button
              v-for="item in overview.weakKnowledgeItems"
              :key="item.knowledgeId"
              class="weak-item"
              type="button"
              @click="goTraining(item.knowledgeId)"
            >
              <div class="weak-main">
                <strong>{{ item.title }}</strong>
                <MasteryBadge :mastery="item.mastery" />
              </div>
              <div v-if="item.tags.length > 0" class="tag-list compact">
                <span v-for="tag in item.tags" :key="`${item.knowledgeId}-${tag}`" class="tag-chip">#{{ tag }}</span>
              </div>
            </button>
          </div>
        </article>

        <article class="section-card">
          <div class="section-head">
            <div>
              <h2>最近训练</h2>
              <span class="section-tip">回看最近一轮问题总结</span>
            </div>
          </div>
          <AppInlineState
            v-if="overview.recentTrainings.length === 0"
            variant="empty"
            text="还没有训练记录，先从知识点列表发起一次训练。"
          />
          <div v-else class="recent-list">
            <button
              v-for="item in overview.recentTrainings"
              :key="item.sessionId"
              class="recent-item"
              type="button"
              @click="goResult(item.sessionId)"
            >
              <div class="recent-top">
                <strong>{{ item.knowledgeTitle }}</strong>
                <div class="recent-badges">
                  <span v-if="item.band" class="pill pill-blue">{{ item.band.label }}</span>
                  <span class="pill pill-dark">{{ item.sessionScore }}</span>
                </div>
              </div>
              <p class="meta-text">完成于 {{ formatDateTime(item.completedAt) }}</p>
            </button>
          </div>
        </article>
      </section>

      <!-- Knowledge List -->
      <AppStateCard
        v-if="knowledgeList.length === 0"
        variant="empty"
        message="暂无知识点，请先新建。"
      />

      <div v-else class="knowledge-list">
        <h2 class="list-title">全部知识点</h2>
        <article v-for="item in knowledgeList" :key="item.id" class="knowledge-card">
          <header class="knowledge-header">
            <div class="title-block">
              <h3>{{ item.title }}</h3>
              <div class="meta-row">
                <span class="pill pill-subtle">{{ sourceTypeLabelMap[item.sourceType || 'MANUAL'] }}</span>
                <span class="meta-text">更新于 {{ formatDateTime(item.updatedAt || item.createdAt) }}</span>
              </div>
            </div>
            <MasteryBadge :mastery="item.mastery" />
          </header>
          <p class="knowledge-content">{{ item.content }}</p>
          <div v-if="item.tags.length > 0" class="tag-list">
            <span v-for="tag in item.tags" :key="`${item.id}-${tag}`" class="tag-chip">#{{ tag }}</span>
          </div>
          <footer class="knowledge-footer">
            <span class="meta-text">创建于 {{ formatDateTime(item.createdAt) }}</span>
            <div class="card-actions">
              <button class="btn btn-primary" type="button" @click="goTraining(item.id)">开始训练</button>
              <button class="btn" type="button" @click="goEdit(item.id)">编辑</button>
              <button class="btn btn-danger" type="button" @click="onDelete(item.id)">归档</button>
            </div>
          </footer>
        </article>
      </div>
    </template>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import AppInlineState from '../components/AppInlineState.vue'
import AppStateCard from '../components/AppStateCard.vue'
import MasteryBadge from '../components/MasteryBadge.vue'
import { dashboardAPI, knowledgeAPI, trainingAPI } from '../api'
import { useTrainingStore } from '../stores/training'
import type {
  DashboardOverview,
  DashboardReviewReminder,
  DashboardReviewReminderItem,
  Knowledge,
  TrainingRecommendation,
  TrainingRecommendationItem
} from '../types'
import { formatDateTime } from '../utils/date'

const router = useRouter()
const trainingStore = useTrainingStore()

const loading = ref(false)
const errorMessage = ref('')
const recommendationLoading = ref(false)
const recommendationError = ref('')
const reviewReminderLoading = ref(false)
const reviewReminderError = ref('')
const startingKnowledgeId = ref<number | null>(null)
const knowledgeList = ref<Knowledge[]>([])
const overview = ref<DashboardOverview>({
  weakKnowledgeItems: [],
  recentTrainings: [],
  progressSummary: {
    trainedCountLast7Days: 0,
    averageScoreLast7Days: 0,
    improvedKnowledgeCount: 0,
  },
})
const recommendation = ref<TrainingRecommendation>({
  packageId: '',
  title: '今日推荐练习',
  items: [],
})
const reviewReminder = ref<DashboardReviewReminder>({
  items: [],
  generatedAt: '',
})

const sourceTypeLabelMap: Record<string, string> = {
  MANUAL: '手动创建',
  BATCH_IMPORT: '批量导入',
  FILE_IMPORT: '文件导入',
  ROLE_GENERATED: '系统生成',
}

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

async function fetchDashboard() {
  loading.value = true
  errorMessage.value = ''
  recommendationError.value = ''
  reviewReminderError.value = ''
  try {
    const [knowledge, summary] = await Promise.all([
      knowledgeAPI.getList(),
      dashboardAPI.getOverview(),
    ])
    knowledgeList.value = knowledge
    overview.value = summary
    await Promise.all([
      fetchTodayRecommendations(),
      fetchReviewReminders(),
    ])
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '获取首页数据失败'
  } finally {
    loading.value = false
  }
}

async function fetchTodayRecommendations() {
  recommendationLoading.value = true
  recommendationError.value = ''
  try {
    recommendation.value = await trainingAPI.getTodayRecommendations()
  } catch (error) {
    recommendation.value = {
      packageId: '',
      title: '今日推荐练习',
      items: [],
    }
    recommendationError.value = error instanceof Error ? error.message : '获取推荐训练包失败'
  } finally {
    recommendationLoading.value = false
  }
}

async function fetchReviewReminders() {
  reviewReminderLoading.value = true
  reviewReminderError.value = ''
  try {
    reviewReminder.value = await dashboardAPI.getReviewReminders()
  } catch (error) {
    reviewReminder.value = {
      items: [],
      generatedAt: '',
    }
    reviewReminderError.value = error instanceof Error ? error.message : '获取回练提醒失败'
  } finally {
    reviewReminderLoading.value = false
  }
}

function goCreate() { router.push('/knowledge/new') }
function goEdit(id: number) { router.push(`/knowledge/edit/${id}`) }
function goImport() { router.push('/knowledge/import') }
function goInvitations() { router.push('/invitations') }
function goTraining(id: number) { router.push(`/training/${id}`) }
function goResult(sessionId: string) { router.push(`/result/${sessionId}`) }
function goHistory() { router.push('/history') }

function resolveKnowledgeTitle(knowledgeId: number) {
  const knowledge = knowledgeList.value.find(item => item.id === knowledgeId)
  return knowledge ? knowledge.title : `知识点 #${knowledgeId}`
}

async function startRecommendedTraining(item: TrainingRecommendationItem) {
  startingKnowledgeId.value = item.knowledgeId
  errorMessage.value = ''
  try {
    const session = await trainingAPI.startSession({
      knowledgeId: item.knowledgeId,
      questionType: item.questionType,
      difficulty: item.difficulty,
      hintEnabled: true,
    })
    trainingStore.setCurrentSession(session)
    trainingStore.setCurrentAnswer('')
    trainingStore.setCurrentHint('')
    trainingStore.setLatestFeedback(null)
    trainingStore.setLatestDetail(null)
    await router.push(`/training/session/${session.sessionId}`)
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '创建推荐训练失败'
  } finally {
    startingKnowledgeId.value = null
  }
}

async function startReviewTraining(item: DashboardReviewReminderItem) {
  startingKnowledgeId.value = item.knowledgeId
  errorMessage.value = ''
  try {
    const session = await trainingAPI.startSession({
      knowledgeId: item.knowledgeId,
      questionType: item.suggestedQuestionType,
      difficulty: item.suggestedDifficulty,
      hintEnabled: true,
    })
    trainingStore.setCurrentSession(session)
    trainingStore.setCurrentAnswer('')
    trainingStore.setCurrentHint('')
    trainingStore.setLatestFeedback(null)
    trainingStore.setLatestDetail(null)
    await router.push(`/training/session/${session.sessionId}`)
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '创建回练会话失败'
  } finally {
    startingKnowledgeId.value = null
  }
}

async function onDelete(id: number) {
  if (!window.confirm('确认归档该知识点？已产生的训练记录会保留。')) {
    return
  }
  try {
    await knowledgeAPI.delete(id)
    await fetchDashboard()
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '删除失败'
  }
}

onMounted(fetchDashboard)
</script>

<style scoped>
.dashboard-page {
  display: grid;
  gap: var(--sp-5);
}

/* Header */
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--sp-4);
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

.header-actions {
  display: flex;
  gap: var(--sp-2);
  flex-shrink: 0;
}

/* Metrics */
.overview-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--sp-4);
}

.metric-card {
  border-radius: var(--radius-lg);
  padding: var(--sp-5);
  display: flex;
  gap: var(--sp-4);
  align-items: flex-start;
  border: 1px solid var(--clr-border);
  transition: transform var(--duration-normal) var(--ease-out), box-shadow var(--duration-normal) var(--ease-out);
  animation: slideUp 0.4s var(--ease-out) both;
}

.metric-card:nth-child(1) { animation-delay: 0ms; }
.metric-card:nth-child(2) { animation-delay: 80ms; }
.metric-card:nth-child(3) { animation-delay: 160ms; }

.metric-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-lg);
}

.metric-blue {
  background: linear-gradient(135deg, #eef2ff, #e0e7ff);
  border-color: #c7d2fe;
}
.metric-green {
  background: linear-gradient(135deg, #ecfdf5, #d1fae5);
  border-color: #a7f3d0;
}
.metric-purple {
  background: linear-gradient(135deg, #faf5ff, #f3e8ff);
  border-color: #e9d5ff;
}

.metric-icon {
  font-size: 1.8rem;
  width: 48px;
  height: 48px;
  display: grid;
  place-items: center;
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.7);
  flex-shrink: 0;
}

.metric-body {
  min-width: 0;
}

.metric-label {
  font-size: var(--fs-xs);
  letter-spacing: 0.08em;
  text-transform: uppercase;
  font-weight: 700;
  color: var(--clr-text-secondary);
}

.metric-card strong {
  display: block;
  margin-top: var(--sp-2);
  font-size: var(--fs-4xl);
  font-weight: 800;
  line-height: 1;
  letter-spacing: -0.02em;
}

.metric-card p {
  margin: var(--sp-2) 0 0;
  color: var(--clr-text-secondary);
  font-size: var(--fs-xs);
  line-height: 1.4;
}

/* Section Cards */
.section-card {
  background: var(--clr-surface);
  border: 1px solid var(--clr-border);
  border-radius: var(--radius-lg);
  padding: var(--sp-5);
  box-shadow: var(--shadow-sm);
}

.recommend-section {
  border-left: 3px solid var(--clr-primary);
}

.review-section {
  border-left: 3px solid var(--clr-accent);
}

.section-head {
  display: flex;
  justify-content: space-between;
  gap: var(--sp-3);
  align-items: flex-start;
}

.section-head h2 {
  margin: 0;
  font-size: var(--fs-xl);
  font-weight: 700;
}

.section-tip {
  font-size: var(--fs-xs);
  color: var(--clr-text-tertiary);
  margin-top: 2px;
  display: block;
}

/* Item Lists */
.item-list,
.weak-list,
.recent-list {
  display: grid;
  gap: var(--sp-3);
  margin-top: var(--sp-4);
}

.list-item,
.weak-item,
.recent-item {
  width: 100%;
  border: 1px solid var(--clr-border);
  border-radius: var(--radius-md);
  background: var(--clr-surface);
  padding: var(--sp-4);
  text-align: left;
  transition: all var(--duration-fast) var(--ease-out);
}

.list-item:hover,
.weak-item:hover,
.recent-item:hover {
  border-color: var(--clr-primary-light);
  box-shadow: var(--shadow-sm);
  transform: translateX(2px);
}

.list-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--sp-3);
}

.review-item {
  flex-direction: column;
  align-items: stretch;
}

.review-item .btn {
  align-self: flex-end;
  margin-top: var(--sp-2);
}

.item-main {
  min-width: 0;
}

.item-main strong {
  font-size: var(--fs-base);
}

.item-reason {
  margin: var(--sp-1) 0;
  font-size: var(--fs-sm);
  color: var(--clr-text-secondary);
}

.item-meta {
  display: flex;
  flex-wrap: wrap;
  gap: var(--sp-2);
  margin-top: var(--sp-2);
}

.weak-main,
.recent-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--sp-2);
}

.recent-badges {
  display: flex;
  gap: var(--sp-2);
}

/* Pills */
.pill {
  display: inline-flex;
  align-items: center;
  min-height: 26px;
  padding: 3px 10px;
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

.pill-subtle {
  background: var(--clr-bg-secondary);
  color: var(--clr-text-secondary);
}

/* Tags */
.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: var(--sp-2);
  margin-bottom: var(--sp-3);
}

.tag-list.compact {
  margin-top: var(--sp-2);
  margin-bottom: 0;
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
  font-weight: 600;
}

/* Knowledge List */
.list-title {
  font-size: var(--fs-xl);
  font-weight: 700;
  margin: 0;
}

.knowledge-list {
  display: grid;
  gap: var(--sp-4);
}

.knowledge-card {
  background: var(--clr-surface);
  border: 1px solid var(--clr-border);
  border-radius: var(--radius-lg);
  padding: var(--sp-5);
  box-shadow: var(--shadow-sm);
  transition: all var(--duration-normal) var(--ease-out);
}

.knowledge-card:hover {
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
}

.knowledge-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--sp-3);
}

.title-block {
  min-width: 0;
}

.knowledge-header h3 {
  margin: 0;
  font-size: var(--fs-xl);
  font-weight: 700;
}

.meta-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--sp-2);
  margin-top: var(--sp-1);
}

.knowledge-content {
  color: var(--clr-text-secondary);
  margin: var(--sp-3) 0 var(--sp-4);
  white-space: pre-wrap;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  font-size: var(--fs-sm);
  line-height: 1.7;
}

.knowledge-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--sp-3);
}

.card-actions {
  display: flex;
  gap: var(--sp-2);
}

.meta-text {
  color: var(--clr-text-tertiary);
  font-size: var(--fs-xs);
  margin: 0;
}

/* Summary Grid */
.summary-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--sp-4);
}

/* Responsive */
@media (max-width: 900px) {
  .overview-grid {
    grid-template-columns: 1fr;
  }
  .summary-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
  }
  .header-actions {
    width: 100%;
    flex-wrap: wrap;
  }
  .knowledge-footer {
    flex-direction: column;
    align-items: flex-start;
  }
  .card-actions {
    width: 100%;
    flex-wrap: wrap;
  }
  .weak-main,
  .recent-top,
  .list-item {
    flex-direction: column;
    align-items: flex-start;
  }
  .section-head {
    flex-direction: column;
  }
}
</style>
