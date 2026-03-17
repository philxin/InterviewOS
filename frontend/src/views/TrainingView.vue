<template>
  <section class="starter-page">
    <div v-if="loading" class="card starter-card">
      <h1>正在创建训练会话...</h1>
      <p>系统会基于当前知识点生成一题，并自动跳转到正式答题页。</p>
    </div>

    <div v-else-if="errorMessage" class="card starter-card error">
      <h1>启动训练失败</h1>
      <p>{{ errorMessage }}</p>
      <div class="actions">
        <button class="btn" type="button" @click="goHome">返回知识点</button>
        <button class="btn btn-primary" type="button" @click="startSession">重试</button>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { trainingAPI } from '../api'
import { useTrainingStore } from '../stores/training'

const route = useRoute()
const router = useRouter()
const trainingStore = useTrainingStore()

const loading = ref(false)
const errorMessage = ref('')
const knowledgeId = computed(() => Number(route.params.knowledgeId))

async function startSession() {
  if (!Number.isFinite(knowledgeId.value) || knowledgeId.value <= 0) {
    errorMessage.value = '无效的 knowledgeId'
    return
  }

  loading.value = true
  errorMessage.value = ''
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
  }
}

function goHome() {
  router.push('/')
}

onMounted(startSession)
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

.error {
  color: #b91c1c;
  border-color: #fecaca;
  background: #fff1f2;
}

.actions {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}
</style>
