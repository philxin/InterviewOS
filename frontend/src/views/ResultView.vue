<template>
  <section class="result-page">
    <header class="page-header">
      <h1>评分结果</h1>
      <p v-if="trainingStore.knowledgeTitle">知识点：{{ trainingStore.knowledgeTitle }}</p>
    </header>

    <div v-if="!evaluation" class="card state-card">
      <p>暂无评分结果，请先完成一次训练。</p>
      <button class="btn btn-primary" type="button" @click="goHome">返回知识点</button>
    </div>

    <div v-else class="result-content">
      <section class="card score-card">
        <h2>综合得分：{{ evaluation.overall }}/100</h2>
        <div class="metric">
          <span>准确度</span>
          <div class="bar"><div class="fill" :style="{ width: `${evaluation.accuracy}%` }" /></div>
          <strong>{{ evaluation.accuracy }}</strong>
        </div>
        <div class="metric">
          <span>深度</span>
          <div class="bar"><div class="fill" :style="{ width: `${evaluation.depth}%` }" /></div>
          <strong>{{ evaluation.depth }}</strong>
        </div>
        <div class="metric">
          <span>清晰度</span>
          <div class="bar"><div class="fill" :style="{ width: `${evaluation.clarity}%` }" /></div>
          <strong>{{ evaluation.clarity }}</strong>
        </div>
      </section>

      <section class="card detail-card">
        <h2>优点</h2>
        <p>{{ evaluation.strengths || '暂无' }}</p>

        <h2>待改进</h2>
        <p>{{ evaluation.weaknesses || '暂无' }}</p>

        <h2>建议</h2>
        <ul v-if="evaluation.suggestions.length > 0">
          <li v-for="(item, index) in evaluation.suggestions" :key="index">{{ item }}</li>
        </ul>
        <p v-else>暂无建议</p>

        <h2>示例回答</h2>
        <pre>{{ evaluation.exampleAnswer || '暂无示例回答' }}</pre>
      </section>

      <footer class="actions">
        <button class="btn" type="button" @click="goHome">返回知识点</button>
        <button
          v-if="trainingStore.knowledgeId"
          class="btn btn-primary"
          type="button"
          @click="retryTraining(trainingStore.knowledgeId)"
        >
          再训练一次
        </button>
        <button class="btn" type="button" @click="goHistory">查看历史</button>
      </footer>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useTrainingStore } from '../stores/training'

const router = useRouter()
const trainingStore = useTrainingStore()
const evaluation = computed(() => trainingStore.evaluation)

function goHome() {
  router.push('/')
}

function goHistory() {
  router.push('/history')
}

function retryTraining(knowledgeId: number) {
  router.push(`/training/${knowledgeId}`)
}
</script>

<style scoped>
.result-page {
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
  display: flex;
  gap: 10px;
  align-items: center;
  justify-content: space-between;
}

.result-content {
  display: grid;
  gap: 12px;
}

.score-card,
.detail-card {
  padding: 16px;
}

.score-card h2,
.detail-card h2 {
  margin: 0 0 10px;
  font-size: 18px;
}

.metric {
  display: grid;
  grid-template-columns: 80px 1fr 40px;
  gap: 8px;
  align-items: center;
  margin-top: 8px;
}

.bar {
  height: 10px;
  border-radius: 999px;
  background: #e2e8f0;
  overflow: hidden;
}

.fill {
  height: 100%;
  background: linear-gradient(90deg, #2563eb 0%, #0ea5e9 100%);
}

.detail-card p {
  margin: 0 0 10px;
  color: #334155;
}

.detail-card ul {
  margin: 0 0 10px;
  padding-left: 18px;
}

pre {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 10px;
  white-space: pre-wrap;
  margin: 0;
}

.actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}

@media (max-width: 768px) {
  .state-card {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
