<template>
  <section class="file-import-page">
    <header class="page-header">
      <div>
        <h1>📄 文件导入知识点</h1>
        <p>上传 txt、md 或 pdf 文件，系统会异步解析并生成知识点。</p>
      </div>
      <div class="actions">
        <button class="btn" type="button" @click="goToBatchImport">批量录入</button>
        <button class="btn" type="button" @click="goBack">返回知识点</button>
      </div>
    </header>

    <section class="card helper-card">
      <h2>导入规则</h2>
      <ul>
        <li>支持格式：txt、md、pdf</li>
        <li>单文件最大 5MB</li>
        <li>文件名会作为知识点标题基础</li>
      </ul>
    </section>

    <section class="card upload-card">
      <label class="field">
        <span>选择文件</span>
        <input accept=".txt,.md,.pdf,text/plain,text/markdown,application/pdf" type="file" @change="onFileChange" />
      </label>

      <div v-if="selectedFile" class="file-meta">
        <strong>📎 {{ selectedFile.name }}</strong>
        <span>{{ formatSize(selectedFile.size) }}</span>
      </div>

      <label class="field">
        <span>默认标签</span>
        <input
          v-model.trim="defaultTagsInput"
          maxlength="500"
          placeholder="例如：redis, cache, persistence"
        />
      </label>

      <div v-if="normalizedTags.length > 0" class="tag-preview">
        <span v-for="tag in normalizedTags" :key="tag" class="tag-chip">
          {{ tag }}
        </span>
      </div>

      <p v-if="errorMessage" class="error">{{ errorMessage }}</p>

      <div class="footer-actions">
        <button class="btn" type="button" :disabled="submitting" @click="goBack">取消</button>
        <button class="btn btn-primary" type="button" :disabled="submitting" @click="submitFile">
          {{ submitting ? '创建任务中...' : '开始文件导入' }}
        </button>
      </div>
    </section>

    <section v-if="task" class="card status-card">
      <header class="status-header">
        <div>
          <h2>导入任务状态</h2>
          <p class="meta-text">任务 ID：{{ task.importId }}</p>
        </div>
        <span class="status-chip" :class="task.status.toLowerCase()">{{ statusLabelMap[task.status] }}</span>
      </header>

      <dl class="status-grid">
        <div>
          <dt>文件</dt>
          <dd>{{ task.fileName }}</dd>
        </div>
        <div>
          <dt>大小</dt>
          <dd>{{ formatSize(task.fileSize) }}</dd>
        </div>
        <div>
          <dt>类型</dt>
          <dd>{{ task.contentType }}</dd>
        </div>
        <div>
          <dt>创建知识点数</dt>
          <dd>{{ task.createdCount }}</dd>
        </div>
        <div>
          <dt>创建时间</dt>
          <dd>{{ formatDate(task.createdAt) }}</dd>
        </div>
        <div>
          <dt>更新时间</dt>
          <dd>{{ formatDate(task.updatedAt) }}</dd>
        </div>
      </dl>

      <div v-if="task.defaultTags.length > 0" class="tag-preview">
        <span v-for="tag in task.defaultTags" :key="`task-${tag}`" class="tag-chip">
          {{ tag }}
        </span>
      </div>

      <p v-if="task.failureReason" class="error">{{ task.failureReason }}</p>

      <div class="status-actions">
        <button class="btn" type="button" @click="refreshStatus">🔄 刷新状态</button>
        <button
          v-if="task.status === 'SUCCESS'"
          class="btn btn-primary"
          type="button"
          @click="goBack"
        >
          返回知识点列表
        </button>
      </div>
    </section>
  </section>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { knowledgeAPI } from '../api'
import type { KnowledgeFileImportResponse } from '../types'

const route = useRoute()
const router = useRouter()

const selectedFile = ref<File | null>(null)
const defaultTagsInput = ref('')
const submitting = ref(false)
const errorMessage = ref('')
const task = ref<KnowledgeFileImportResponse | null>(null)
let pollingTimer: number | null = null

const statusLabelMap: Record<KnowledgeFileImportResponse['status'], string> = {
  PENDING: '等待处理',
  PROCESSING: '处理中',
  SUCCESS: '已完成',
  FAILED: '失败',
}

const normalizedTags = computed(() =>
  Array.from(
    new Set(
      defaultTagsInput.value
        .split(',')
        .map((item) => item.trim().toLowerCase())
        .filter(Boolean)
    )
  )
)

onMounted(async () => {
  const importId = resolveImportIdFromRoute()
  if (importId) {
    await fetchTask(importId)
    schedulePolling()
  }
})

onBeforeUnmount(() => { stopPolling() })

function resolveImportIdFromRoute() {
  return typeof route.query.importId === 'string' ? route.query.importId : ''
}

function onFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  selectedFile.value = input.files?.[0] ?? null
  errorMessage.value = ''
}

function validate() {
  if (!selectedFile.value) { errorMessage.value = '请选择要导入的文件'; return false }
  const lowerName = selectedFile.value.name.toLowerCase()
  if (!lowerName.endsWith('.txt') && !lowerName.endsWith('.md') && !lowerName.endsWith('.pdf')) {
    errorMessage.value = '仅支持 txt、md、pdf 文件'
    return false
  }
  if (selectedFile.value.size > 5 * 1024 * 1024) { errorMessage.value = '文件大小不能超过 5MB'; return false }
  const invalidTag = normalizedTags.value.find((tag) => tag.length > 50)
  if (invalidTag) { errorMessage.value = `标签长度不能超过 50：${invalidTag}`; return false }
  return true
}

async function submitFile() {
  if (!validate() || !selectedFile.value) return
  submitting.value = true
  errorMessage.value = ''
  try {
    const response = await knowledgeAPI.fileImport(selectedFile.value, defaultTagsInput.value)
    await router.replace({ path: '/knowledge/file-import', query: { importId: response.importId } })
    await fetchTask(response.importId)
    schedulePolling()
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '文件导入任务创建失败'
  } finally {
    submitting.value = false
  }
}

async function refreshStatus() {
  const importId = task.value?.importId || resolveImportIdFromRoute()
  if (!importId) return
  await fetchTask(importId)
}

async function fetchTask(importId: string) {
  try {
    task.value = await knowledgeAPI.getFileImportStatus(importId)
    errorMessage.value = ''
    if (task.value.status === 'SUCCESS' || task.value.status === 'FAILED') stopPolling()
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '获取任务状态失败'
    stopPolling()
  }
}

function schedulePolling() {
  stopPolling()
  if (!task.value || task.value.status === 'SUCCESS' || task.value.status === 'FAILED') return
  pollingTimer = window.setInterval(() => {
    const importId = task.value?.importId
    if (!importId) { stopPolling(); return }
    void fetchTask(importId)
  }, 2000)
}

function stopPolling() {
  if (pollingTimer !== null) { window.clearInterval(pollingTimer); pollingTimer = null }
}

function formatSize(size: number) {
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / (1024 * 1024)).toFixed(2)} MB`
}

function formatDate(value: string | null) {
  if (!value) return '—'
  return new Date(value).toLocaleString('zh-CN', { hour12: false })
}

function goBack() { router.push('/') }
function goToBatchImport() { router.push('/knowledge/import') }
</script>

<style scoped>
.file-import-page {
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

.actions,
.footer-actions,
.status-actions {
  display: flex;
  gap: var(--sp-2);
}

.helper-card,
.upload-card,
.status-card {
  padding: var(--sp-5);
}

.helper-card h2,
.status-card h2 {
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

.field {
  display: grid;
  gap: 6px;
}

.upload-card {
  display: grid;
  gap: var(--sp-4);
}

.field span {
  font-size: var(--fs-sm);
  font-weight: 700;
  color: var(--clr-text-secondary);
}

.field input[type="text"],
.field input[type="file"] {
  font-size: var(--fs-sm);
}

.field input:not([type="file"]) {
  width: 100%;
  border: 1.5px solid var(--clr-border);
  border-radius: var(--radius-md);
  padding: 12px 16px;
  background: var(--clr-surface);
  transition: all var(--duration-fast) var(--ease-out);
}

.field input:not([type="file"]):focus {
  outline: none;
  border-color: var(--clr-primary);
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.1);
}

.file-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--sp-3) var(--sp-4);
  border-radius: var(--radius-md);
  background: var(--clr-bg-secondary);
  color: var(--clr-text);
  font-size: var(--fs-sm);
}

.status-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--sp-3);
}

.meta-text {
  margin: var(--sp-1) 0 0;
  color: var(--clr-text-tertiary);
  font-size: var(--fs-xs);
}

.status-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: var(--sp-3);
  margin: var(--sp-4) 0 0;
}

.status-grid dt {
  font-size: var(--fs-xs);
  font-weight: 700;
  color: var(--clr-text-tertiary);
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.status-grid dd {
  margin: 4px 0 0;
  color: var(--clr-text);
  font-size: var(--fs-sm);
  font-weight: 600;
}

.tag-preview {
  display: flex;
  flex-wrap: wrap;
  gap: var(--sp-2);
}

.tag-chip {
  display: inline-flex;
  align-items: center;
  padding: 4px 12px;
  border-radius: var(--radius-full);
  background: var(--clr-primary-50);
  color: var(--clr-primary-dark);
  font-size: var(--fs-xs);
  font-weight: 700;
}

.status-chip {
  display: inline-flex;
  align-items: center;
  padding: 6px 14px;
  border-radius: var(--radius-full);
  font-size: var(--fs-xs);
  font-weight: 700;
  flex-shrink: 0;
}

.status-chip.pending {
  background: var(--clr-warning-bg);
  color: #b45309;
}

.status-chip.processing {
  background: var(--clr-primary-100);
  color: var(--clr-primary-dark);
  animation: pulse 2s ease-in-out infinite;
}

.status-chip.success {
  background: var(--clr-success-bg);
  color: #15803d;
}

.status-chip.failed {
  background: var(--clr-danger-bg);
  color: var(--clr-danger);
}

.error {
  margin: 0;
  color: var(--clr-danger);
  font-size: var(--fs-sm);
  font-weight: 500;
}

@media (max-width: 768px) {
  .page-header,
  .status-header {
    flex-direction: column;
  }
  .actions,
  .footer-actions,
  .status-actions {
    flex-wrap: wrap;
  }
}
</style>
