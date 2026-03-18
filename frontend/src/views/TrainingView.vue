<template>
  <section class="starter-page">
    <div v-if="loading" class="card starter-card">
      <h1>正在创建训练会话...</h1>
      <p>{{ loadingMessage }}</p>
      <div class="progress-meta">
        <span class="status-chip">LLM 生成中</span>
        <span>已等待 {{ elapsedSeconds }} 秒</span>
      </div>
    </div>

    <AppStateCard
      v-else-if="errorMessage"
      class="starter-card"
      variant="error"
      title="启动训练失败"
      :message="errorMessage"
    >
      <template #actions>
        <div class="actions">
          <button class="btn" type="button" @click="goHome">返回知识点</button>
          <button class="btn btn-primary" type="button" @click="startSession">重试</button>
        </div>
      </template>
    </AppStateCard>
  </section>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { trainingAPI } from '../api'
import AppStateCard from '../components/AppStateCard.vue'
import { useTrainingStore } from '../stores/training'

const route = useRoute()
const router = useRouter()
const trainingStore = useTrainingStore()

const loading = ref(false)
const errorMessage = ref('')
const elapsedSeconds = ref(0)
const knowledgeId = computed(() => Number(route.params.knowledgeId))
let loadingTimer: number | null = null

const loadingMessage = computed(() => {
  if (elapsedSeconds.value < 4) {
    return '系统会基于当前知识点生成一题，并自动跳转到正式答题页。'
  }
  if (elapsedSeconds.value < 10) {
    return '正在组织题目上下文并生成正式问题，通常还需要几秒。'
  }
  return '题目生成时间较长，系统仍在处理中，请保持当前页面。'
})

function startLoadingTimer() {
  stopLoadingTimer()
  elapsedSeconds.value = 0
  loadingTimer = window.setInterval(() => {
    elapsedSeconds.value += 1
  }, 1000)
}

function stopLoadingTimer() {
  if (loadingTimer !== null) {
    window.clearInterval(loadingTimer)
    loadingTimer = null
  }
}

async function startSession() {
  if (!Number.isFinite(knowledgeId.value) || knowledgeId.value <= 0) {
    errorMessage.value = '无效的 knowledgeId'
    return
  }

  loading.value = true
  errorMessage.value = ''
  startLoadingTimer()
  try {
    const session = await trainingAPI.startSession({
      knowledgeId: knowledgeId.value,
      difficulty: 'MEDIUM',
      hintEnabled: true,
    })
    trainingStore.setCurrentSession(session)
    trainingStore.setCurrentAnswer('')
    trainingStore.setCurrentHint('')
    trainingStore.setLatestFeedback(null)
    trainingStore.setLatestDetail(null)
    await router.replace(`/training/session/${session.sessionId}`)
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '启动训练失败'
  } finally {
    loading.value = false
    stopLoadingTimer()
  }
}

function goHome() {
  router.push('/')
}

onMounted(startSession)
onBeforeUnmount(stopLoadingTimer)
</script>

<style scoped>
.starter-page {
  display: grid;
  place-items: center;
  min-height: 50vh;
}

.starter-card {
  width: min(640px, 100%);
  padding: 28px;
  display: grid;
  gap: 10px;
}

.starter-card h1,
.starter-card p {
  margin: 0;
}

.progress-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  color: #475569;
  font-size: 14px;
}

.status-chip {
  display: inline-flex;
  align-items: center;
  border-radius: 999px;
  padding: 4px 10px;
  background: #ecfeff;
  color: #155e75;
  font-weight: 700;
}

.actions {
  display: flex;
  gap: 8px;
}
</style>
