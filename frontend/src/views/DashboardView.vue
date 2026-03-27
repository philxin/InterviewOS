<template>
  <section class="dashboard-page">
    <header class="hero-panel">
      <div class="hero-copy">
        <span class="hero-kicker">InterviewOS Dashboard</span>
        <div class="header-content">
          <h1>知识点训练看板</h1>
          <p>把推荐训练、回练提醒和知识点维护收敛到一个首页，首屏直接进入当天最该处理的内容。</p>
        </div>
        <div class="header-actions">
          <button class="btn" type="button" @click="goInvitations">✉️ 邀请注册</button>
          <button class="btn" type="button" @click="goHistory">📊 训练历史</button>
          <button class="btn" type="button" @click="goImport">📥 批量导入</button>
          <button class="btn btn-primary" type="button" @click="goCreate">➕ 新建知识点</button>
        </div>
      </div>

      <aside class="hero-spotlight">
        <span class="spotlight-label">今日焦点</span>
        <strong>{{ focusHeadline }}</strong>
        <p>{{ focusDescription }}</p>
        <div class="spotlight-stats">
          <article class="spotlight-stat">
            <span>知识点总数</span>
            <strong>{{ totalKnowledgeCount }}</strong>
          </article>
          <article class="spotlight-stat">
            <span>推荐训练</span>
            <strong>{{ recommendationCount }}</strong>
          </article>
          <article class="spotlight-stat">
            <span>待回练</span>
            <strong>{{ pendingReviewCount }}</strong>
          </article>
        </div>
      </aside>
    </header>

    <AppStateCard v-if="loading && !dashboardReady" variant="loading" message="正在加载首页概览..." />
    <AppStateCard v-else-if="errorMessage && !dashboardReady" variant="error" :message="errorMessage" />
    <template v-else>
      <AppInlineState v-if="errorMessage" variant="error" :text="errorMessage" />

      <section class="overview-grid">
        <article class="metric-card metric-blue">
          <div class="metric-icon">🎯</div>
          <div class="metric-body">
            <span class="metric-label">近 7 天训练次数</span>
            <strong>{{ overview.progressSummary.trainedCountLast7Days }}</strong>
            <p>保持训练频率，先保证稳定输出。</p>
          </div>
        </article>
        <article class="metric-card metric-green">
          <div class="metric-icon">📈</div>
          <div class="metric-body">
            <span class="metric-label">近 7 天平均分</span>
            <strong>{{ overview.progressSummary.averageScoreLast7Days }}</strong>
            <p>把平均表现稳定住，再追求单次高分。</p>
          </div>
        </article>
        <article class="metric-card metric-purple">
          <div class="metric-icon">⚡</div>
          <div class="metric-body">
            <span class="metric-label">本周提升知识点</span>
            <strong>{{ overview.progressSummary.improvedKnowledgeCount }}</strong>
            <p>近 7 天掌握度出现正向变化的主题数。</p>
          </div>
        </article>
      </section>

      <section class="workspace-grid">
        <div class="workspace-main">
          <section class="section-card recommend-section">
            <div class="section-head">
              <div>
                <h2>{{ recommendation.title || '今日推荐练习' }}</h2>
                <span class="section-tip">系统基于薄弱项和最近训练表现生成，适合作为今天的第一轮训练。</span>
              </div>
              <span class="panel-stat">{{ recommendationCount }} 个建议</span>
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
        </div>

        <aside class="workspace-side">
          <section class="section-card review-section">
            <div class="section-head">
              <div>
                <h2>回练提醒</h2>
                <span class="section-tip">结合掌握度、最近表现和训练间隔动态更新，优先避免遗忘。</span>
              </div>
              <span class="panel-stat">{{ pendingReviewCount }} 个待处理</span>
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

          <section class="summary-stack">
            <article class="section-card">
              <div class="section-head">
                <div>
                  <h2>当前薄弱项</h2>
                  <span class="section-tip">优先复练掌握度最低的知识点，缩短“会一点但说不清”的区间。</span>
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
                  <span class="section-tip">快速回看上一轮结果，判断是否需要接着补练。</span>
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
        </aside>
      </section>

      <AppStateCard
        v-if="knowledgeList.length === 0"
        variant="empty"
        message="暂无知识点，请先新建。"
      />

      <section v-else class="section-card knowledge-section">
        <div class="section-head knowledge-head">
          <div>
            <h2>全部知识点</h2>
            <span class="section-tip">当前库中的知识点清单，可直接开始训练、编辑或归档。</span>
          </div>
          <span class="panel-stat">{{ totalKnowledgeCount }} 个主题</span>
        </div>
        <div class="knowledge-list">
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
      </section>
    </template>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
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
const dashboardReady = ref(false)
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

const totalKnowledgeCount = computed(() => knowledgeList.value.length)
const recommendationCount = computed(() => recommendation.value.items.length)
const pendingReviewCount = computed(() => reviewReminder.value.items.length)

const focusHeadline = computed(() => {
  const firstReviewItem = reviewReminder.value.items[0]
  if (firstReviewItem) {
    return firstReviewItem.knowledgeTitle
  }

  const firstRecommendationItem = recommendation.value.items[0]
  if (firstRecommendationItem) {
    return resolveKnowledgeTitle(firstRecommendationItem.knowledgeId)
  }

  const firstWeakKnowledgeItem = overview.value.weakKnowledgeItems[0]
  if (firstWeakKnowledgeItem) {
    return firstWeakKnowledgeItem.title
  }

  return '先创建一个知识点'
})

const focusDescription = computed(() => {
  const firstReviewItem = reviewReminder.value.items[0]
  if (firstReviewItem) {
    return firstReviewItem.reason
  }

  const firstRecommendationItem = recommendation.value.items[0]
  if (firstRecommendationItem) {
    return `建议先做 ${questionTypeLabelMap[firstRecommendationItem.questionType]} / ${difficultyLabelMap[firstRecommendationItem.difficulty]} 训练，快速进入状态。`
  }

  if (overview.value.weakKnowledgeItems[0]) {
    return '当前已有薄弱项，但还缺少足够的推荐包数据，可以直接从薄弱项入口继续补练。'
  }

  return '当前还没有足够训练数据，先录入一个知识点并发起第一轮训练。'
})

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
    dashboardReady.value = true
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

.hero-panel {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1.6fr) minmax(320px, 0.95fr);
  gap: var(--sp-4);
  padding: var(--sp-6);
  border: 1px solid rgba(148, 163, 184, 0.28);
  border-radius: var(--radius-2xl);
  background:
    radial-gradient(circle at top left, rgba(99, 102, 241, 0.16), transparent 36%),
    radial-gradient(circle at bottom right, rgba(6, 182, 212, 0.16), transparent 34%),
    linear-gradient(135deg, rgba(255, 255, 255, 0.96), rgba(248, 250, 252, 0.96));
  box-shadow: var(--shadow-md);
  overflow: hidden;
}

.hero-panel::after {
  content: '';
  position: absolute;
  inset: auto -80px -120px auto;
  width: 280px;
  height: 280px;
  border-radius: 50%;
  background: rgba(99, 102, 241, 0.08);
  filter: blur(10px);
}

.hero-copy,
.hero-spotlight {
  position: relative;
  z-index: 1;
}

.hero-copy {
  display: grid;
  align-content: start;
  gap: var(--sp-5);
}

.hero-kicker {
  display: inline-flex;
  align-items: center;
  width: fit-content;
  min-height: 32px;
  padding: 0 14px;
  border-radius: var(--radius-full);
  background: rgba(255, 255, 255, 0.84);
  border: 1px solid rgba(99, 102, 241, 0.14);
  color: var(--clr-primary-dark);
  font-size: var(--fs-xs);
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.header-content h1 {
  margin: 0;
  font-size: clamp(2rem, 4vw, 3rem);
  font-weight: 800;
  line-height: 1.05;
  letter-spacing: -0.03em;
}

.header-content p {
  max-width: 56ch;
  margin: var(--sp-3) 0 0;
  color: var(--clr-text-secondary);
  font-size: var(--fs-base);
}

.header-actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--sp-2);
}

.hero-spotlight {
  display: grid;
  gap: var(--sp-4);
  align-content: start;
  padding: var(--sp-5);
  border-radius: var(--radius-xl);
  background: linear-gradient(160deg, rgba(15, 23, 42, 0.95), rgba(30, 41, 59, 0.92));
  color: var(--clr-text-inverse);
  box-shadow: var(--shadow-lg);
}

.spotlight-label {
  color: rgba(255, 255, 255, 0.72);
  font-size: var(--fs-xs);
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.hero-spotlight > strong {
  font-size: var(--fs-2xl);
  font-weight: 800;
  line-height: 1.15;
  letter-spacing: -0.02em;
}

.hero-spotlight > p {
  margin: 0;
  color: rgba(255, 255, 255, 0.8);
  font-size: var(--fs-sm);
  line-height: 1.6;
}

.spotlight-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--sp-2);
}

.spotlight-stat {
  display: grid;
  gap: var(--sp-1);
  padding: var(--sp-3);
  border-radius: var(--radius-md);
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: rgba(255, 255, 255, 0.06);
}

.spotlight-stat span {
  font-size: var(--fs-xs);
  color: rgba(255, 255, 255, 0.64);
}

.spotlight-stat strong {
  font-size: var(--fs-xl);
  font-weight: 800;
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
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
  line-height: 1.5;
}

.workspace-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(320px, 0.95fr);
  gap: var(--sp-4);
  align-items: start;
}

.workspace-main,
.workspace-side,
.summary-stack {
  display: grid;
  gap: var(--sp-4);
}

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
  display: block;
  margin-top: 2px;
  color: var(--clr-text-tertiary);
  font-size: var(--fs-xs);
  line-height: 1.5;
}

.panel-stat {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 0 12px;
  border-radius: var(--radius-full);
  background: var(--clr-bg-secondary);
  color: var(--clr-text-secondary);
  font-size: var(--fs-xs);
  font-weight: 700;
  white-space: nowrap;
}

.item-list,
.weak-list,
.recent-list,
.knowledge-list {
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

.knowledge-section {
  display: grid;
  gap: var(--sp-2);
}

.knowledge-head {
  margin-bottom: var(--sp-1);
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

@media (max-width: 1100px) {
  .hero-panel,
  .workspace-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 900px) {
  .overview-grid {
    grid-template-columns: 1fr;
  }

  .spotlight-stats {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .hero-panel {
    padding: var(--sp-5);
  }

  .header-actions {
    width: 100%;
  }

  .header-actions .btn {
    flex: 1 1 180px;
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
  .list-item,
  .section-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .panel-stat {
    white-space: normal;
  }

  .review-item .btn {
    width: 100%;
    align-self: stretch;
  }
}

@media (max-width: 560px) {
  .hero-panel {
    padding: var(--sp-4);
  }

  .hero-spotlight {
    padding: var(--sp-4);
  }

  .spotlight-stats {
    grid-template-columns: 1fr;
  }

  .knowledge-header,
  .card-actions {
    flex-direction: column;
    align-items: flex-start;
  }

  .card-actions .btn {
    width: 100%;
  }
}
</style>
