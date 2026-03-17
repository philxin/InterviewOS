<template>
  <section class="import-page">
    <header class="page-header">
      <div>
        <h1>批量导入知识点</h1>
        <p>一次录入多条知识点，适合把现有笔记快速整理进系统。</p>
      </div>
      <div class="actions">
        <button class="btn" type="button" @click="goBack">返回知识点</button>
        <button class="btn" type="button" @click="addItem">+ 新增一行</button>
      </div>
    </header>

    <section class="card helper-card">
      <h2>录入说明</h2>
      <ul>
        <li>标题和内容必填。</li>
        <li>标签使用英文逗号分隔，前端会先做 `trim + lowercase + de-duplicate`。</li>
        <li>服务端允许部分成功；失败项会按原始序号返回。</li>
      </ul>
    </section>

    <div class="batch-list">
      <article v-for="(item, index) in items" :key="item.id" class="card batch-card">
        <header class="card-header">
          <h2>第 {{ index + 1 }} 条</h2>
          <button v-if="items.length > 1" class="btn btn-danger" type="button" @click="removeItem(item.id)">
            删除
          </button>
        </header>

        <label class="field">
          <span>标题</span>
          <input v-model.trim="item.title" maxlength="200" placeholder="例如：Redis 持久化" />
        </label>

        <label class="field">
          <span>内容</span>
          <textarea
            v-model.trim="item.content"
            rows="6"
            placeholder="请输入知识点内容、核心原理、注意事项或面试答法"
          />
        </label>

        <label class="field">
          <span>标签</span>
          <input
            v-model.trim="item.tagsInput"
            maxlength="500"
            placeholder="例如：redis, cache, persistence"
          />
        </label>

        <div v-if="normalizeTags(item.tagsInput).length > 0" class="tag-preview">
          <span
            v-for="tag in normalizeTags(item.tagsInput)"
            :key="`${item.id}-${tag}`"
            class="tag-chip"
          >
            {{ tag }}
          </span>
        </div>
      </article>
    </div>

    <p v-if="errorMessage" class="error">{{ errorMessage }}</p>

    <section v-if="lastResult" class="card result-card">
      <h2>最近一次导入结果</h2>
      <p>成功 {{ lastResult.createdCount }} 条，失败 {{ lastResult.failedCount }} 条。</p>

      <div v-if="lastResult.failedItems.length > 0" class="failed-list">
        <article
          v-for="failed in lastResult.failedItems"
          :key="`${failed.index}-${failed.title}`"
          class="failed-item"
        >
          <strong>第 {{ failed.index + 1 }} 条 · {{ failed.title || '未填写标题' }}</strong>
          <span>{{ failed.reason }}</span>
        </article>
      </div>
    </section>

    <footer class="footer-actions">
      <button class="btn" type="button" :disabled="submitting" @click="goBack">取消</button>
      <button class="btn btn-primary" type="button" :disabled="submitting" @click="submitBatch">
        {{ submitting ? '导入中...' : '开始批量导入' }}
      </button>
    </footer>
  </section>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { knowledgeAPI } from '../api'
import { ApiRequestError } from '../api/client'
import type { BatchImportKnowledgeItemRequest, BatchImportKnowledgeResponse } from '../types'

type EditableItem = {
  id: number
  title: string
  content: string
  tagsInput: string
}

const router = useRouter()

const errorMessage = ref('')
const submitting = ref(false)
const lastResult = ref<BatchImportKnowledgeResponse | null>(null)
const nextId = ref(2)
const items = ref<EditableItem[]>([
  {
    id: 1,
    title: '',
    content: '',
    tagsInput: '',
  },
])

function normalizeTags(tagsInput: string) {
  return Array.from(
    new Set(
      tagsInput
        .split(',')
        .map((item) => item.trim().toLowerCase())
        .filter(Boolean)
    )
  )
}

function addItem() {
  items.value.push({
    id: nextId.value,
    title: '',
    content: '',
    tagsInput: '',
  })
  nextId.value += 1
}

function removeItem(id: number) {
  items.value = items.value.filter((item) => item.id !== id)
}

function validateItems() {
  const hasValidItem = items.value.some((item) => item.title || item.content || item.tagsInput)
  if (!hasValidItem) {
    errorMessage.value = '至少填写一条知识点'
    return false
  }

  for (const [index, item] of items.value.entries()) {
    if (!item.title.trim()) {
      errorMessage.value = `第 ${index + 1} 条标题不能为空`
      return false
    }
    if (!item.content.trim()) {
      errorMessage.value = `第 ${index + 1} 条内容不能为空`
      return false
    }
    const invalidTag = normalizeTags(item.tagsInput).find((tag) => tag.length > 50)
    if (invalidTag) {
      errorMessage.value = `第 ${index + 1} 条标签长度不能超过 50：${invalidTag}`
      return false
    }
  }
  return true
}

function buildPayload(): BatchImportKnowledgeItemRequest[] {
  return items.value.map((item) => ({
    title: item.title.trim(),
    content: item.content.trim(),
    tags: normalizeTags(item.tagsInput),
  }))
}

async function submitBatch() {
  if (!validateItems()) {
    return
  }

  submitting.value = true
  errorMessage.value = ''
  try {
    const result = await knowledgeAPI.batchImport({
      items: buildPayload(),
    })
    lastResult.value = result
    if (result.failedCount === 0) {
      await router.replace('/')
    }
  } catch (error) {
    if (error instanceof ApiRequestError && error.status === 422 && error.payload) {
      lastResult.value = error.payload as BatchImportKnowledgeResponse
      errorMessage.value = error.message
    } else if (error instanceof Error) {
      errorMessage.value = error.message
    } else {
      errorMessage.value = '批量导入失败'
    }
  } finally {
    submitting.value = false
  }
}

function goBack() {
  router.push('/')
}
</script>

<style scoped>
.import-page {
  display: grid;
  gap: 16px;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.page-header h1 {
  margin: 0;
  font-size: 30px;
}

.page-header p {
  margin: 8px 0 0;
  color: #64748b;
}

.actions {
  display: flex;
  gap: 8px;
}

.helper-card,
.result-card {
  padding: 16px;
}

.helper-card h2,
.result-card h2 {
  margin: 0 0 10px;
  font-size: 18px;
}

.helper-card ul {
  margin: 0;
  padding-left: 18px;
  display: grid;
  gap: 6px;
  color: #334155;
}

.batch-list {
  display: grid;
  gap: 14px;
}

.batch-card {
  padding: 16px;
  display: grid;
  gap: 14px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}

.card-header h2 {
  margin: 0;
  font-size: 18px;
}

.field {
  display: grid;
  gap: 6px;
}

.field span {
  font-size: 13px;
  font-weight: 700;
  color: #334155;
}

.field input,
.field textarea {
  width: 100%;
  border: 1px solid #cbd5e1;
  border-radius: 10px;
  padding: 10px 12px;
  background: #fff;
}

.field textarea {
  resize: vertical;
}

.tag-preview {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
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

.error {
  margin: 0;
  color: #b91c1c;
}

.failed-list {
  display: grid;
  gap: 10px;
  margin-top: 12px;
}

.failed-item {
  display: grid;
  gap: 4px;
  padding: 12px;
  border-radius: 12px;
  background: #fff1f2;
  border: 1px solid #fecaca;
  color: #881337;
}

.footer-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

@media (max-width: 768px) {
  .page-header,
  .footer-actions {
    flex-direction: column;
    align-items: flex-start;
  }

  .actions {
    width: 100%;
    flex-wrap: wrap;
  }

  .card-header {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
