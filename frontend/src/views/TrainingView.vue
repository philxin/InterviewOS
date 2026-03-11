<template>
  <section class="training-page">
    <header class="page-header">
      <h1>训练中：{{ knowledgeTitle || `知识点 #${knowledgeId}` }}</h1>
      <p>先由 AI 生成问题，再提交你的回答进行评分。</p>
    </header>

    <div v-if="loading" class="card state-card">正在准备训练题目...</div>
    <div v-else-if="errorMessage" class="card state-card error">{{ errorMessage }}</div>

    <div v-else class="card training-card">
      <div class="section">
        <h2>问题</h2>
        <p class="question">{{ question }}</p>
      </div>

      <div class="section">
        <h2>你的回答</h2>
        <textarea
          v-model.trim="answer"
          rows="10"
          placeholder="请输入你的回答，尽量覆盖原理、流程和实践点。"
        />
      </div>

      <div class="actions">
        <button class="btn" type="button" :disabled="submitting" @click="goBack">取消</button>
        <button class="btn btn-primary" type="button" :disabled="submitting || !answer" @click="submitAnswer">
          {{ submitting ? '评分中...' : '提交并评分' }}
        </button>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { knowledgeAPI, trainingAPI } from '../api'
import { useTrainingStore } from '../stores/training'

const route = useRoute()
const router = useRouter()
const trainingStore = useTrainingStore()

const knowledgeId = computed(() => Number(route.params.knowledgeId))
const knowledgeTitle = ref('')
const question = ref('')
const answer = ref('')
const loading = ref(false)
const submitting = ref(false)
const errorMessage = ref('')

async function initTraining() {
  if (!Number.isFinite(knowledgeId.value) || knowledgeId.value <= 0) {
    errorMessage.value = '无效的 knowledgeId'
    return
  }

  loading.value = true
  errorMessage.value = ''
  try {
    const knowledge = await knowledgeAPI.getById(knowledgeId.value)
    knowledgeTitle.value = knowledge.title
    const response = await trainingAPI.start({ knowledgeId: knowledgeId.value })
    question.value = response.question
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '启动训练失败'
  } finally {
    loading.value = false
  }
}

async function submitAnswer() {
  if (!answer.value || !question.value) {
    errorMessage.value = '问题或回答不能为空'
    return
  }

  submitting.value = true
  errorMessage.value = ''
  try {
    const evaluation = await trainingAPI.submit({
      knowledgeId: knowledgeId.value,
      question: question.value,
      answer: answer.value,
    })
    trainingStore.setSession({
      knowledgeId: knowledgeId.value,
      knowledgeTitle: knowledgeTitle.value,
      question: question.value,
      answer: answer.value,
      evaluation,
    })
    router.push('/result')
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '提交评分失败'
  } finally {
    submitting.value = false
  }
}

function goBack() {
  router.push('/')
}

onMounted(initTraining)
</script>

<style scoped>
.training-page {
  display: grid;
  gap: 16px;
}

.page-header h1 {
  margin: 0;
  font-size: 28px;
}

.page-header p {
  margin: 6px 0 0;
  color: #64748b;
}

.state-card {
  padding: 20px;
}

.error {
  color: #b91c1c;
  border-color: #fecaca;
  background: #fff1f2;
}

.training-card {
  padding: 16px;
  display: grid;
  gap: 16px;
}

.section {
  display: grid;
  gap: 8px;
}

.section h2 {
  margin: 0;
  font-size: 18px;
}

.question {
  margin: 0;
  color: #334155;
  white-space: pre-wrap;
}

textarea {
  width: 100%;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  padding: 10px;
  resize: vertical;
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>
