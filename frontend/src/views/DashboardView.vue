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

    <div v-if="loading" class="card state-card">正在加载首页概览...</div>
    <div v-else-if="errorMessage" class="card state-card error">{{ errorMessage }}</div>
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

      <section class="summary-grid">
        <article class="card summary-card">
          <div class="section-head">
            <h2>当前薄弱项</h2>
            <span class="section-tip">优先复练掌握度最低的知识点</span>
          </div>
          <div v-if="overview.weakKnowledgeItems.length === 0" class="empty-copy">
            暂无知识点数据，先创建 1-2 个训练主题。
          </div>
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
          <div v-if="overview.recentTrainings.length === 0" class="empty-copy">
            还没有训练记录，先从知识点列表发起一次训练。
          </div>
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

      <div v-if="knowledgeList.length === 0" class="card state-card">暂无知识点，请先新建。</div>

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
import MasteryBadge from '../components/MasteryBadge.vue'
import { dashboardAPI, knowledgeAPI } from '../api'
import type { DashboardOverview, Knowledge } from '../types'
import { formatDateTime } from '../utils/date'

const router = useRouter()

const loading = ref(false)
const errorMessage = ref('')
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

const sourceTypeLabelMap: Record<string, string> = {
  MANUAL: '手动创建',
  BATCH_IMPORT: '批量导入',
  FILE_IMPORT: '文件导入',
  ROLE_GENERATED: '系统生成',
}

async function fetchDashboard() {
  loading.value = true
  errorMessage.value = ''
  try {
    const [knowledge, summary] = await Promise.all([
      knowledgeAPI.getList(),
      dashboardAPI.getOverview(),
    ])
    knowledgeList.value = knowledge
    overview.value = summary
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '获取首页数据失败'
  } finally {
    loading.value = false
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
.knowledge-card,
.state-card {
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

.empty-copy {
  margin-top: 16px;
  color: #64748b;
}

.weak-list,
.recent-list {
  display: grid;
  gap: 10px;
  margin-top: 16px;
}

.weak-item,
.recent-item {
  width: 100%;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  background: #fff;
  padding: 14px;
  text-align: left;
}

.weak-main,
.recent-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
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

.error {
  color: #b91c1c;
  border-color: #fecaca;
  background: #fff1f2;
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
  .section-head {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
