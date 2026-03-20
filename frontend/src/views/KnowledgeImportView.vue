<template>
  <section class="import-page">
    <header class="page-header">
      <div>
        <h1>📥 批量导入知识点</h1>
        <p>一次录入多条知识点，适合把现有笔记快速整理进系统。</p>
      </div>
      <div class="actions">
        <button class="btn" type="button" @click="goBack">返回知识点</button>
        <button class="btn" type="button" @click="goToFileImport">文件导入</button>
        <button class="btn btn-primary" type="button" @click="addItem">➕ 新增一行</button>
      </div>
    </header>

    <section class="card helper-card">
      <h2>录入说明</h2>
      <ul>
        <li>标题和内容必填。</li>
        <li>标签使用英文逗号分隔。</li>
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
  { id: 1, title: '', content: '', tagsInput: '' },
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
  items.value.push({ id: nextId.value, title: '', content: '', tagsInput: '' })
  nextId.value += 1
}

function removeItem(id: number) {
  items.value = items.value.filter((item) => item.id !== id)
}

function validateItems() {
  const hasValidItem = items.value.some((item) => item.title || item.content || item.tagsInput)
  if (!hasValidItem) { errorMessage.value = '至少填写一条知识点'; return false }
  for (const [index, item] of items.value.entries()) {
    if (!item.title.trim()) { errorMessage.value = `第 ${index + 1} 条标题不能为空`; return false }
    if (!item.content.trim()) { errorMessage.value = `第 ${index + 1} 条内容不能为空`; return false }
    const invalidTag = normalizeTags(item.tagsInput).find((tag) => tag.length > 50)
    if (invalidTag) { errorMessage.value = `第 ${index + 1} 条标签长度不能超过 50：${invalidTag}`; return false }
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
  if (!validateItems()) return
  submitting.value = true
  errorMessage.value = ''
  try {
    const result = await knowledgeAPI.batchImport({ items: buildPayload() })
    lastResult.value = result
    if (result.failedCount === 0) await router.replace('/')
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

function goBack() { router.push('/') }
function goToFileImport() { router.push('/knowledge/file-import') }
</script>

<style scoped>
.import-page {
  display: grid;
  gap: var(--sp-5);
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--sp-3);
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

.actions {
  display: flex;
  gap: var(--sp-2);
  flex-shrink: 0;
}

.helper-card,
.result-card {
  padding: var(--sp-5);
}

.helper-card h2,
.result-card h2 {
  margin: 0 0 var(--sp-3);
  font-size: var(--fs-lg);
  font-weight: 700;
}

.helper-card ul {
  margin: 0;
  padding-left: 18px;
  display: grid;
  gap: var(--sp-2);
  color: var(--clr-text-secondary);
  font-size: var(--fs-sm);
}

.batch-list {
  display: grid;
  gap: var(--sp-4);
}

.batch-card {
  padding: var(--sp-5);
  display: grid;
  gap: var(--sp-4);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--sp-3);
}

.card-header h2 {
  margin: 0;
  font-size: var(--fs-lg);
  font-weight: 700;
}

.field {
  display: grid;
  gap: 6px;
}

.field span {
  font-size: var(--fs-sm);
  font-weight: 700;
  color: var(--clr-text-secondary);
}

.field input,
.field textarea {
  width: 100%;
  border: 1.5px solid var(--clr-border);
  border-radius: var(--radius-md);
  padding: 12px 16px;
  background: var(--clr-surface);
  font-size: var(--fs-sm);
  transition: all var(--duration-fast) var(--ease-out);
}

.field input:focus,
.field textarea:focus {
  outline: none;
  border-color: var(--clr-primary);
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.1);
}

.field textarea {
  resize: vertical;
  line-height: 1.7;
}

.tag-preview {
  display: flex;
  flex-wrap: wrap;
  gap: var(--sp-2);
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

.error {
  margin: 0;
  color: var(--clr-danger);
  font-weight: 500;
}

.failed-list {
  display: grid;
  gap: var(--sp-3);
  margin-top: var(--sp-3);
}

.failed-item {
  display: grid;
  gap: 4px;
  padding: var(--sp-3);
  border-radius: var(--radius-md);
  background: var(--clr-danger-bg);
  border: 1px solid var(--clr-danger-border);
  color: #881337;
  font-size: var(--fs-sm);
}

.footer-actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--sp-2);
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
  }
  .actions {
    width: 100%;
    flex-wrap: wrap;
  }
  .footer-actions {
    flex-direction: column;
  }
  .footer-actions .btn {
    width: 100%;
  }
}
</style>
