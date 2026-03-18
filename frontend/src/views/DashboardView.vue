<template>
  <section class="dashboard-page">
    <header class="page-header">
      <div>
        <h1>知识点训练看板</h1>
        <p>首页先给出进步概览，再进入知识点管理和训练入口。</p>
      </div>
      <div class="actions">
        <button class="btn" type="button" @click="goHistory">训练历史</button>
        <button class="btn" type="button" @click="goImport">批量导入</button>
        <button class="btn btn-primary" type="button" @click="goCreate">+ 新建知识点</button>
      </div>
    </header>

    <AppStateCard v-if="loading" variant="loading" message="正在加载首页概览..." />
    <AppStateCard v-else-if="errorMessage" variant="error" :message="errorMessage" />
    <template v-else>
      <section class="overview-grid">
        <article class="card metric-card">
          <span class="metric-label">近 7 天训练次数</span>
          <strong>{{ overview.progressSummary.trainedCountLast7Days }}</strong>
          <p>保持节奏比一次高分更重要。</p>
        </article>
        <article class="card metric-card">
          <span class="metric-label">近 7 天平均分</span>
          <strong>{{ overview.progressSummary.averageScoreLast7Days }}</strong>
          <p>先把平均稳定到 60+，再追求更高档位。</p>
        </article>
        <article class="card metric-card">
          <span class="metric-label">本周提升知识点</span>
          <strong>{{ overview.progressSummary.improvedKnowledgeCount }}</strong>
          <p>统计近 7 天掌握度提升过的知识点数量。</p>
        </article>
      </section>

      <section class="card recommendation-card">
        <div class="section-head">
          <h2>{{ recommendation.title || '今日推荐练习' }}</h2>
          <span class="section-tip">系统基于薄弱项和最近训练表现生成 3-5 题</span>
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
        <div v-else class="recommendation-list">
          <article
            v-for="item in recommendation.items"
            :key="`recommend-${item.knowledgeId}-${item.questionType}-${item.difficulty}`"
            class="recommendation-item"
          >
            <div class="recommendation-main">
              <strong>{{ resolveKnowledgeTitle(item.knowledgeId) }}</strong>
              <div class="recommendation-meta">
                <span class="band-pill">{{ questionTypeLabelMap[item.questionType] }}</span>
                <span class="score-pill">{{ difficultyLabelMap[item.difficulty] }}</span>
              </div>
            </div>
            <button
              class="btn btn-primary"
              type="button"
              :disabled="startingKnowledgeId === item.knowledgeId"
              @click="startRecommendedTraining(item)"
            >
              {{ startingKnowledgeId === item.knowledgeId ? '创建中...' : '开始推荐训练' }}
            </button>
          </article>
        </div>
      </section>

      <section class="card recommendation-card">
        <div class="section-head">
          <h2>回练提醒</h2>
          <span class="section-tip">结合掌握度、最近表现和训练间隔动态更新优先级</span>
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
        <div v-else class="recommendation-list">
          <article
            v-for="item in reviewReminder.items"
            :key="`review-${item.knowledgeId}`"
            class="recommendation-item"
          >
            <div class="recommendation-main reminder-main">
              <strong>{{ item.knowledgeTitle }}</strong>
              <p class="meta">{{ item.reason }}</p>
              <div class="recommendation-meta">
                <span class="score-pill">权重 {{ item.reviewWeight }}</span>
                <span class="band-pill">{{ questionTypeLabelMap[item.suggestedQuestionType] }}</span>
                <span class="band-pill">{{ difficultyLabelMap[item.suggestedDifficulty] }}</span>
              </div>
              <p class="meta">
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

      <section class="summary-grid">
        <article class="card summary-card">
          <div class="section-head">
            <h2>当前薄弱项</h2>
            <span class="section-tip">优先复练掌握度最低的知识点</span>
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

        <article class="card summary-card">
          <div class="section-head">
            <h2>最近训练</h2>
            <span class="section-tip">优先回看最近一轮问题总结</span>
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
                  <span v-if="item.band" class="band-pill">{{ item.band.label }}</span>
                  <span class="score-pill">{{ item.sessionScore }}</span>
                </div>
              </div>
              <p class="meta">完成于 {{ formatDateTime(item.completedAt) }}</p>
            </button>
          </div>
        </article>
      </section>

      <AppStateCard
        v-if="knowledgeList.length === 0"
        variant="empty"
        message="暂无知识点，请先新建。"
      />

      <div v-else class="knowledge-list">
        <article v-for="item in knowledgeList" :key="item.id" class="card knowledge-card">
          <header class="knowledge-header">
            <div class="title-block">
              <h2>{{ item.title }}</h2>
              <div class="meta-row">
                <span class="source-pill">{{ sourceTypeLabelMap[item.sourceType || 'MANUAL'] }}</span>
                <span class="meta">更新于 {{ formatDateTime(item.updatedAt || item.createdAt) }}</span>
              </div>
            </div>
            <MasteryBadge :mastery="item.mastery" />
          </header>
          <p class="knowledge-content">{{ item.content }}</p>
          <div v-if="item.tags.length > 0" class="tag-list">
            <span v-for="tag in item.tags" :key="`${item.id}-${tag}`" class="tag-chip">#{{ tag }}</span>
          </div>
          <footer class="knowledge-footer">
            <span class="meta">创建于 {{ formatDateTime(item.createdAt) }}</span>
            <div class="actions">
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

function goCreate() {
  router.push('/knowledge/new')
}

function goEdit(id: number) {
  router.push(`/knowledge/edit/${id}`)
}

function goImport() {
  router.push('/knowledge/import')
}

function goTraining(id: number) {
  router.push(`/training/${id}`)
}

function goResult(sessionId: string) {
  router.push(`/result/${sessionId}`)
}

function goHistory() {
  router.push('/history')
}

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
  gap: 16px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.page-header h1 {
  margin: 0;
  font-size: 28px;
  line-height: 1.2;
}

.page-header p {
  margin: 6px 0 0;
  color: #64748b;
}

.actions {
  display: flex;
  gap: 8px;
}

.overview-grid,
.summary-grid,
.knowledge-list {
  display: grid;
  gap: 12px;
}

.overview-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.summary-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.metric-card,
.summary-card,
.recommendation-card,
.knowledge-card {
  padding: 18px;
}

.metric-label,
.section-tip {
  font-size: 12px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  font-weight: 700;
}

.metric-label {
  color: #0f766e;
}

.metric-card strong {
  display: block;
  margin-top: 14px;
  font-size: 36px;
  line-height: 1;
}

.metric-card p {
  margin: 12px 0 0;
  color: #64748b;
}

.section-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.section-head h2 {
  margin: 0;
  font-size: 20px;
}

.section-tip {
  color: #94a3b8;
}

.weak-list,
.recent-list,
.recommendation-list {
  display: grid;
  gap: 10px;
  margin-top: 16px;
}

.weak-item,
.recent-item,
.recommendation-item {
  width: 100%;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  background: #fff;
  padding: 14px;
  text-align: left;
}

.weak-main,
.recent-top,
.recommendation-main {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}

.reminder-main {
  display: grid;
  justify-items: start;
  gap: 8px;
}

.recommendation-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.recommendation-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.compact {
  margin-top: 10px;
}

.recent-badges {
  display: flex;
  align-items: center;
  gap: 8px;
}

.band-pill,
.score-pill {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
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

.knowledge-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.title-block {
  display: grid;
  gap: 6px;
}

.knowledge-header h2 {
  margin: 0;
  font-size: 20px;
}

.meta-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.source-pill {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  padding: 0 8px;
  border-radius: 999px;
  background: #f1f5f9;
  color: #475569;
  font-size: 12px;
  font-weight: 700;
}

.knowledge-content {
  color: #334155;
  margin: 10px 0 14px;
  white-space: pre-wrap;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
}

.knowledge-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 14px;
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

.meta {
  color: #64748b;
  font-size: 13px;
}

@media (max-width: 900px) {
  .overview-grid,
  .summary-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .page-header,
  .knowledge-footer,
  .weak-main,
  .recent-top,
  .recommendation-main,
  .recommendation-item,
  .section-head {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
