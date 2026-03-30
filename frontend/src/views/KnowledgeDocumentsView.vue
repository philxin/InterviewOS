<template>
  <section class="documents-page">
    <header class="page-header">
      <div>
        <h1>文档知识库</h1>
        <p>查看当前用户已索引文档，并通过最小检索入口验证召回质量。</p>
      </div>
      <div class="header-actions">
        <button class="btn" type="button" @click="goFileImport">文件导入</button>
        <button class="btn" type="button" @click="goBack">返回看板</button>
      </div>
    </header>

    <AppStateCard v-if="loading" variant="loading" message="正在加载文档列表..." />
    <AppStateCard v-else-if="errorMessage" variant="error" :message="errorMessage" />

    <template v-else>
      <section class="layout-grid">
        <article class="card documents-card">
          <header class="section-head">
            <h2>文档列表</h2>
            <span class="panel-stat">{{ documents.length }} 个文档</span>
          </header>

          <AppStateCard
            v-if="documents.length === 0"
            variant="empty"
            message="暂无文档，请先完成一次文件导入。"
          />

          <div v-else class="documents-list">
            <button
              v-for="item in documents"
              :key="item.documentId"
              type="button"
              class="document-item"
              :class="{ active: item.documentId === selectedDocumentId }"
              @click="selectDocument(item.documentId)"
            >
              <div class="document-main">
                <strong>{{ item.title }}</strong>
                <span class="meta-text">{{ item.originalFileName }}</span>
              </div>
              <div class="item-side">
                <span class="status-chip" :class="item.status.toLowerCase()">{{ documentStatusLabelMap[item.status] }}</span>
                <span class="meta-text">{{ item.activeChunks }} / {{ item.totalChunks }} chunks</span>
              </div>
            </button>
          </div>
        </article>

        <article class="card detail-card">
          <header class="section-head">
            <h2>文档详情</h2>
          </header>

          <AppInlineState
            v-if="!selectedDocument"
            variant="empty"
            text="请选择一个文档查看详情。"
          />

          <template v-else>
            <dl class="detail-grid">
              <div>
                <dt>文档 ID</dt>
                <dd>{{ selectedDocument.documentId }}</dd>
              </div>
              <div>
                <dt>导入任务 ID</dt>
                <dd>{{ selectedDocument.importId || '—' }}</dd>
              </div>
              <div>
                <dt>状态</dt>
                <dd>
                  <span class="status-chip" :class="selectedDocument.status.toLowerCase()">
                    {{ documentStatusLabelMap[selectedDocument.status] }}
                  </span>
                </dd>
              </div>
              <div>
                <dt>索引进度</dt>
                <dd>{{ selectedDocument.activeChunks }} / {{ selectedDocument.totalChunks }} chunks</dd>
              </div>
              <div>
                <dt>Embedding 模型</dt>
                <dd>{{ selectedDocument.embeddingModel || '—' }}</dd>
              </div>
              <div>
                <dt>向量维度</dt>
                <dd>{{ selectedDocument.embeddingDim ?? '—' }}</dd>
              </div>
              <div>
                <dt>索引完成时间</dt>
                <dd>{{ formatDate(selectedDocument.indexedAt) }}</dd>
              </div>
              <div>
                <dt>更新时间</dt>
                <dd>{{ formatDate(selectedDocument.updatedAt) }}</dd>
              </div>
            </dl>

            <section class="concepts-section">
              <header class="section-head">
                <h2>候选知识点</h2>
                <span class="panel-stat">{{ concepts.length }} 条</span>
              </header>

              <AppInlineState
                v-if="conceptsLoading"
                variant="loading"
                text="正在加载候选知识点..."
              />
              <AppInlineState
                v-else-if="conceptsError"
                variant="error"
                :text="conceptsError"
              />
              <AppInlineState
                v-else-if="concepts.length === 0"
                variant="empty"
                text="当前文档暂无候选知识点。"
              />

              <ul v-else class="concept-list">
                <li v-for="concept in concepts" :key="concept.conceptId" class="concept-item">
                  <div class="concept-head">
                    <strong>{{ concept.name }}</strong>
                    <div class="concept-head-meta">
                      <span class="status-chip" :class="concept.status.toLowerCase()">
                        {{ conceptStatusLabelMap[concept.status] }}
                      </span>
                      <span v-if="concept.confidence !== null" class="score-pill">
                        置信度 {{ concept.confidence.toFixed(3) }}
                      </span>
                    </div>
                  </div>

                  <p class="result-snippet">
                    {{ concept.summary || '暂无来源摘要。' }}
                  </p>
                  <p class="meta-text">
                    来源片段 chunkIds：{{ concept.supportingChunkIds.length > 0 ? concept.supportingChunkIds.join(', ') : '—' }}
                  </p>

                  <div v-if="concept.aliases.length > 0" class="tag-preview">
                    <span v-for="alias in concept.aliases" :key="`${concept.conceptId}-${alias}`" class="tag-chip">
                      {{ alias }}
                    </span>
                  </div>

                  <p v-if="concept.status === 'ACCEPTED'" class="meta-text">
                    已接受并关联知识点 ID：{{ concept.acceptedKnowledgeId ?? '—' }}
                  </p>

                  <div v-if="concept.status === 'CANDIDATE'" class="concept-actions-wrap">
                    <label class="field">
                      <span>接受标题（可选）</span>
                      <input v-model.trim="draftOf(concept).title" maxlength="200" placeholder="默认使用候选名称" />
                    </label>
                    <label class="field">
                      <span>补充内容（可选）</span>
                      <textarea
                        v-model.trim="draftOf(concept).content"
                        rows="3"
                        maxlength="1000"
                        placeholder="默认使用候选摘要"
                      />
                    </label>
                    <label class="field">
                      <span>标签（可选）</span>
                      <input v-model.trim="draftOf(concept).tags" maxlength="300" placeholder="例如：redis, cache" />
                    </label>

                    <div class="concept-actions">
                      <button
                        class="btn btn-primary"
                        type="button"
                        :disabled="conceptActionLoading === concept.conceptId"
                        @click="acceptConceptAsNew(concept)"
                      >
                        {{ conceptActionLoading === concept.conceptId ? '处理中...' : '确认并新建' }}
                      </button>
                      <button
                        class="btn"
                        type="button"
                        :disabled="conceptActionLoading === concept.conceptId"
                        @click="rejectConcept(concept)"
                      >
                        忽略
                      </button>
                    </div>

                    <div class="merge-row">
                      <select v-model="draftOf(concept).mergeKnowledgeId">
                        <option value="">合并到已有知识点...</option>
                        <option
                          v-for="knowledge in knowledgeOptions"
                          :key="`merge-${concept.conceptId}-${knowledge.id}`"
                          :value="String(knowledge.id)"
                        >
                          {{ knowledge.title }}
                        </option>
                      </select>
                      <button
                        class="btn"
                        type="button"
                        :disabled="conceptActionLoading === concept.conceptId || !draftOf(concept).mergeKnowledgeId"
                        @click="acceptConceptAsMerge(concept)"
                      >
                        合并确认
                      </button>
                    </div>
                  </div>
                </li>
              </ul>
            </section>
          </template>
        </article>
      </section>

      <section class="card search-card">
        <header class="section-head">
          <h2>检索验证</h2>
          <span class="section-tip">用于快速验证文档召回，不进入训练流程。</span>
        </header>

        <div class="search-form">
          <label class="field field-query">
            <span>检索问题</span>
            <textarea
              v-model.trim="searchQuery"
              rows="3"
              maxlength="500"
              placeholder="例如：Spring Boot 自动配置的核心机制是什么？"
            />
          </label>

          <label class="field">
            <span>检索范围</span>
            <select v-model="searchDocumentId">
              <option value="">全部文档</option>
              <option v-for="item in documents" :key="`scope-${item.documentId}`" :value="item.documentId">
                {{ item.title }}
              </option>
            </select>
          </label>

          <label class="field">
            <span>TopK</span>
            <select v-model.number="searchTopK">
              <option :value="3">3</option>
              <option :value="5">5</option>
              <option :value="8">8</option>
              <option :value="10">10</option>
            </select>
          </label>
        </div>

        <p v-if="searchError" class="error-text">{{ searchError }}</p>

        <div class="search-actions">
          <button class="btn" type="button" :disabled="searchLoading" @click="clearSearch">清空结果</button>
          <button class="btn btn-primary" type="button" :disabled="searchLoading" @click="runSearch">
            {{ searchLoading ? '检索中...' : '开始检索' }}
          </button>
        </div>

        <div v-if="searchResult" class="result-block">
          <div class="result-summary">
            <span class="pill pill-dark">命中 {{ searchResult.hitCount }} 条</span>
            <span class="pill" :class="searchResult.degraded ? 'pill-warning' : 'pill-blue'">
              {{ searchResult.degraded ? 'FALLBACK' : 'RAG' }}
            </span>
            <span class="meta-text">topK={{ searchResult.topK }} · query={{ searchResult.query }}</span>
          </div>

          <AppInlineState
            v-if="searchResult.items.length === 0"
            variant="empty"
            text="未命中引用片段，当前检索已降级。请调整查询词或先导入相关文档。"
          />

          <ul v-else class="result-list">
            <li
              v-for="item in searchResult.items"
              :key="`${item.chunkId}-${item.documentId}`"
              class="result-item"
            >
              <div class="result-head">
                <strong>{{ item.documentTitle }}</strong>
                <span class="score-pill">score {{ item.score.toFixed(3) }}</span>
              </div>
              <p class="result-snippet">{{ item.snippet }}</p>
              <p class="meta-text">
                chunk #{{ item.chunkId }}
                <template v-if="item.pageFrom !== null">
                  · 页码 {{ item.pageFrom }}-{{ item.pageTo ?? item.pageFrom }}
                </template>
                <template v-if="item.startOffset !== null">
                  · 偏移 {{ item.startOffset }}-{{ item.endOffset ?? item.startOffset }}
                </template>
              </p>
            </li>
          </ul>
        </div>
      </section>
    </template>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppInlineState from '../components/AppInlineState.vue'
import AppStateCard from '../components/AppStateCard.vue'
import { knowledgeConceptAPI, knowledgeDocumentAPI, knowledgeAPI, ragAPI } from '../api'
import type {
  AcceptKnowledgeConceptRequest,
  Knowledge,
  KnowledgeConcept,
  KnowledgeDocument,
  RagSearchResponse,
} from '../types'
import { formatDateTime } from '../utils/date'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const searchLoading = ref(false)
const errorMessage = ref('')
const searchError = ref('')
const documents = ref<KnowledgeDocument[]>([])
const selectedDocumentId = ref('')
const searchQuery = ref('')
const searchTopK = ref(5)
const searchDocumentId = ref('')
const searchResult = ref<RagSearchResponse | null>(null)
const concepts = ref<KnowledgeConcept[]>([])
const conceptsLoading = ref(false)
const conceptsError = ref('')
const conceptActionLoading = ref<number | null>(null)
const knowledgeOptions = ref<Knowledge[]>([])
const conceptDrafts = ref<Record<number, ConceptDraft>>({})
let conceptFetchSequence = 0

type ConceptDraft = {
  title: string
  content: string
  tags: string
  mergeKnowledgeId: string
}

const selectedDocument = computed(() =>
  documents.value.find((item) => item.documentId === selectedDocumentId.value) || null
)

const documentStatusLabelMap: Record<KnowledgeDocument['status'], string> = {
  PROCESSING: '处理中',
  ACTIVE: '可检索',
  ARCHIVED: '已归档',
  FAILED: '失败',
}

const conceptStatusLabelMap: Record<KnowledgeConcept['status'], string> = {
  CANDIDATE: '待确认',
  ACCEPTED: '已确认',
  REJECTED: '已忽略',
}

function formatDate(value: string | null) {
  if (!value) {
    return '—'
  }
  return formatDateTime(value)
}

function selectDocument(documentId: string) {
  selectedDocumentId.value = documentId
  searchDocumentId.value = documentId
}

async function fetchDocuments() {
  loading.value = true
  errorMessage.value = ''
  try {
    const list = await knowledgeDocumentAPI.getList()
    documents.value = list

    const queryDocumentId = typeof route.query.documentId === 'string' ? route.query.documentId : ''
    const matchedFromQuery = list.find((item) => item.documentId === queryDocumentId)?.documentId || ''
    selectedDocumentId.value = matchedFromQuery || list[0]?.documentId || ''

    if (!searchDocumentId.value && selectedDocumentId.value) {
      searchDocumentId.value = selectedDocumentId.value
    }
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '获取文档列表失败'
  } finally {
    loading.value = false
  }
}

async function fetchKnowledgeOptions() {
  try {
    const list = await knowledgeAPI.getList()
    knowledgeOptions.value = list
      .filter((item) => item.status === 'ACTIVE')
      .sort((left, right) => right.updatedAt.localeCompare(left.updatedAt))
  } catch {
    knowledgeOptions.value = []
  }
}

function draftOf(concept: KnowledgeConcept): ConceptDraft {
  const existing = conceptDrafts.value[concept.conceptId]
  if (existing) {
    return existing
  }
  const nextDraft: ConceptDraft = {
    title: concept.name || '',
    content: concept.summary || '',
    tags: '',
    mergeKnowledgeId: '',
  }
  conceptDrafts.value[concept.conceptId] = nextDraft
  return nextDraft
}

function resetConceptDrafts(list: KnowledgeConcept[]) {
  const nextDrafts: Record<number, ConceptDraft> = {}
  for (const concept of list) {
    const existing = conceptDrafts.value[concept.conceptId]
    nextDrafts[concept.conceptId] = {
      title: existing?.title ?? concept.name ?? '',
      content: existing?.content ?? concept.summary ?? '',
      tags: existing?.tags ?? '',
      mergeKnowledgeId: existing?.mergeKnowledgeId ?? '',
    }
  }
  conceptDrafts.value = nextDrafts
}

async function fetchConcepts(documentId: string) {
  const currentSequence = ++conceptFetchSequence
  conceptsLoading.value = true
  conceptsError.value = ''
  try {
    const list = await knowledgeConceptAPI.getByDocument(documentId)
    if (currentSequence !== conceptFetchSequence) {
      return
    }
    concepts.value = list
    resetConceptDrafts(list)
  } catch (error) {
    if (currentSequence !== conceptFetchSequence) {
      return
    }
    concepts.value = []
    conceptsError.value = error instanceof Error ? error.message : '获取候选知识点失败'
  } finally {
    if (currentSequence === conceptFetchSequence) {
      conceptsLoading.value = false
    }
  }
}

function normalizeTagsInput(rawTags: string) {
  return Array.from(
    new Set(
      rawTags
        .split(',')
        .map((tag) => tag.trim().toLowerCase())
        .filter(Boolean)
    )
  )
}

function buildAcceptPayload(draft: ConceptDraft): AcceptKnowledgeConceptRequest {
  const payload: AcceptKnowledgeConceptRequest = {}
  const title = draft.title.trim()
  const content = draft.content.trim()
  const tags = normalizeTagsInput(draft.tags)
  if (title) {
    payload.title = title
  }
  if (content) {
    payload.content = content
  }
  if (tags.length > 0) {
    payload.tags = tags
  }
  return payload
}

function replaceConcept(updated: KnowledgeConcept) {
  const index = concepts.value.findIndex((item) => item.conceptId === updated.conceptId)
  if (index >= 0) {
    concepts.value[index] = updated
    return
  }
  concepts.value.unshift(updated)
}

async function acceptConceptAsNew(concept: KnowledgeConcept) {
  if (conceptActionLoading.value !== null) {
    return
  }
  conceptActionLoading.value = concept.conceptId
  conceptsError.value = ''
  try {
    const updated = await knowledgeConceptAPI.accept(concept.conceptId, buildAcceptPayload(draftOf(concept)))
    replaceConcept(updated)
    await fetchKnowledgeOptions()
  } catch (error) {
    conceptsError.value = error instanceof Error ? error.message : '确认候选知识点失败'
  } finally {
    conceptActionLoading.value = null
  }
}

async function acceptConceptAsMerge(concept: KnowledgeConcept) {
  if (conceptActionLoading.value !== null) {
    return
  }
  const draft = draftOf(concept)
  const mergeKnowledgeId = Number(draft.mergeKnowledgeId)
  if (!Number.isInteger(mergeKnowledgeId) || mergeKnowledgeId <= 0) {
    conceptsError.value = '请先选择要合并的知识点'
    return
  }

  conceptActionLoading.value = concept.conceptId
  conceptsError.value = ''
  try {
    const payload = buildAcceptPayload(draft)
    payload.mergeKnowledgeId = mergeKnowledgeId
    const updated = await knowledgeConceptAPI.accept(concept.conceptId, payload)
    replaceConcept(updated)
    await fetchKnowledgeOptions()
  } catch (error) {
    conceptsError.value = error instanceof Error ? error.message : '合并候选知识点失败'
  } finally {
    conceptActionLoading.value = null
  }
}

async function rejectConcept(concept: KnowledgeConcept) {
  if (conceptActionLoading.value !== null) {
    return
  }
  conceptActionLoading.value = concept.conceptId
  conceptsError.value = ''
  try {
    const updated = await knowledgeConceptAPI.reject(concept.conceptId)
    replaceConcept(updated)
  } catch (error) {
    conceptsError.value = error instanceof Error ? error.message : '忽略候选知识点失败'
  } finally {
    conceptActionLoading.value = null
  }
}

async function runSearch() {
  if (!searchQuery.value) {
    searchError.value = '请输入检索问题'
    return
  }
  searchLoading.value = true
  searchError.value = ''
  try {
    searchResult.value = await ragAPI.search({
      query: searchQuery.value,
      documentId: searchDocumentId.value || undefined,
      topK: searchTopK.value,
    })
  } catch (error) {
    searchError.value = error instanceof Error ? error.message : '检索失败'
  } finally {
    searchLoading.value = false
  }
}

function clearSearch() {
  searchQuery.value = ''
  searchError.value = ''
  searchResult.value = null
}

function goBack() {
  router.push('/')
}

function goFileImport() {
  router.push('/knowledge/file-import')
}

watch(selectedDocumentId, (documentId) => {
  if (!documentId) {
    concepts.value = []
    conceptsError.value = ''
    conceptsLoading.value = false
    return
  }
  void fetchConcepts(documentId)
})

onMounted(async () => {
  await Promise.all([fetchDocuments(), fetchKnowledgeOptions()])
})
</script>

<style scoped>
.documents-page {
  display: grid;
  gap: var(--sp-5);
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--sp-4);
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

.header-actions {
  display: flex;
  gap: var(--sp-2);
}

.layout-grid {
  display: grid;
  grid-template-columns: minmax(320px, 1.1fr) minmax(0, 1fr);
  gap: var(--sp-4);
}

.documents-card,
.detail-card,
.search-card {
  padding: var(--sp-5);
}

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--sp-3);
}

.section-head h2 {
  margin: 0;
  font-size: var(--fs-lg);
  font-weight: 700;
}

.section-tip {
  font-size: var(--fs-xs);
  color: var(--clr-text-tertiary);
}

.panel-stat {
  display: inline-flex;
  align-items: center;
  min-height: 26px;
  padding: 2px 10px;
  border-radius: var(--radius-full);
  background: var(--clr-bg-secondary);
  color: var(--clr-text-secondary);
  font-size: var(--fs-xs);
  font-weight: 700;
}

.documents-list {
  margin-top: var(--sp-4);
  display: grid;
  gap: var(--sp-2);
  max-height: 420px;
  overflow: auto;
}

.document-item {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: var(--sp-3);
  padding: var(--sp-3) var(--sp-4);
  border-radius: var(--radius-md);
  border: 1px solid var(--clr-border);
  background: var(--clr-surface);
  text-align: left;
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-out);
}

.document-item:hover {
  border-color: var(--clr-primary);
  transform: translateY(-1px);
  box-shadow: var(--shadow-sm);
}

.document-item.active {
  border-color: var(--clr-primary-dark);
  background: var(--clr-primary-50);
}

.document-main {
  display: grid;
  gap: 2px;
}

.document-main strong {
  font-size: var(--fs-sm);
  font-weight: 700;
}

.item-side {
  display: grid;
  gap: 6px;
  justify-items: end;
}

.meta-text {
  margin: 0;
  color: var(--clr-text-tertiary);
  font-size: var(--fs-xs);
}

.status-chip {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  padding: 0 10px;
  border-radius: var(--radius-full);
  font-size: var(--fs-xs);
  font-weight: 700;
}

.status-chip.processing {
  background: var(--clr-warning-bg);
  color: #b45309;
}

.status-chip.active {
  background: var(--clr-success-bg);
  color: #15803d;
}

.status-chip.archived {
  background: var(--clr-bg-secondary);
  color: var(--clr-text-secondary);
}

.status-chip.failed {
  background: var(--clr-danger-bg);
  color: var(--clr-danger);
}

.detail-grid {
  margin: var(--sp-4) 0 0;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: var(--sp-3);
}

.detail-grid dt {
  font-size: var(--fs-xs);
  color: var(--clr-text-tertiary);
  text-transform: uppercase;
  letter-spacing: 0.08em;
  font-weight: 700;
}

.detail-grid dd {
  margin: 4px 0 0;
  font-size: var(--fs-sm);
  color: var(--clr-text);
  word-break: break-all;
}

.concepts-section {
  margin-top: var(--sp-5);
  display: grid;
  gap: var(--sp-3);
}

.concept-list {
  margin: 0;
  padding: 0;
  list-style: none;
  display: grid;
  gap: var(--sp-3);
}

.concept-item {
  padding: var(--sp-3);
  border-radius: var(--radius-md);
  border: 1px solid var(--clr-border);
  background: var(--clr-surface);
}

.concept-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--sp-2);
}

.concept-head strong {
  font-size: var(--fs-base);
  font-weight: 700;
}

.concept-head-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--sp-2);
}

.tag-preview {
  margin-top: var(--sp-2);
  display: flex;
  flex-wrap: wrap;
  gap: var(--sp-2);
}

.tag-chip {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  padding: 0 10px;
  border-radius: var(--radius-full);
  background: var(--clr-primary-50);
  color: var(--clr-primary-dark);
  font-size: var(--fs-xs);
  font-weight: 700;
}

.concept-actions-wrap {
  margin-top: var(--sp-3);
  display: grid;
  gap: var(--sp-2);
}

.concept-actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--sp-2);
}

.merge-row {
  display: flex;
  align-items: center;
  gap: var(--sp-2);
}

.merge-row select {
  flex: 1;
  width: 100%;
  border: 1.5px solid var(--clr-border);
  border-radius: var(--radius-md);
  padding: 10px 12px;
  background: var(--clr-surface);
  font-size: var(--fs-sm);
  transition: all var(--duration-fast) var(--ease-out);
}

.merge-row select:focus {
  outline: none;
  border-color: var(--clr-primary);
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.1);
}

.search-form {
  margin-top: var(--sp-4);
  display: grid;
  grid-template-columns: minmax(0, 2fr) repeat(2, minmax(160px, 0.8fr));
  gap: var(--sp-3);
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
.field textarea,
.field select {
  width: 100%;
  border: 1.5px solid var(--clr-border);
  border-radius: var(--radius-md);
  padding: 10px 12px;
  background: var(--clr-surface);
  font-size: var(--fs-sm);
  transition: all var(--duration-fast) var(--ease-out);
}

.field input:focus,
.field textarea:focus,
.field select:focus {
  outline: none;
  border-color: var(--clr-primary);
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.1);
}

.field-query textarea {
  resize: vertical;
  min-height: 82px;
}

.search-actions {
  margin-top: var(--sp-3);
  display: flex;
  justify-content: flex-end;
  gap: var(--sp-2);
}

.error-text {
  margin: var(--sp-3) 0 0;
  color: var(--clr-danger);
  font-size: var(--fs-sm);
  font-weight: 500;
}

.result-block {
  margin-top: var(--sp-4);
  display: grid;
  gap: var(--sp-3);
}

.result-summary {
  display: flex;
  flex-wrap: wrap;
  gap: var(--sp-2);
  align-items: center;
}

.pill {
  display: inline-flex;
  align-items: center;
  min-height: 26px;
  padding: 0 10px;
  border-radius: var(--radius-full);
  font-size: var(--fs-xs);
  font-weight: 700;
}

.pill-blue {
  background: var(--clr-primary-50);
  color: var(--clr-primary-dark);
}

.pill-warning {
  background: var(--clr-warning-bg);
  color: #b45309;
}

.pill-dark {
  background: var(--clr-text);
  color: var(--clr-text-inverse);
}

.result-list {
  margin: 0;
  padding: 0;
  list-style: none;
  display: grid;
  gap: var(--sp-3);
}

.result-item {
  padding: var(--sp-3);
  border-radius: var(--radius-md);
  border: 1px solid var(--clr-border);
  background: var(--clr-surface);
}

.result-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--sp-2);
}

.result-head strong {
  font-size: var(--fs-sm);
  font-weight: 700;
}

.score-pill {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  padding: 0 10px;
  border-radius: var(--radius-full);
  background: rgba(6, 182, 212, 0.12);
  color: var(--clr-accent-dark);
  font-size: var(--fs-xs);
  font-weight: 700;
}

.result-snippet {
  margin: var(--sp-2) 0 0;
  color: var(--clr-text-secondary);
  font-size: var(--fs-sm);
  line-height: 1.6;
}

@media (max-width: 900px) {
  .page-header,
  .header-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .layout-grid {
    grid-template-columns: 1fr;
  }

  .search-form {
    grid-template-columns: 1fr;
  }

  .merge-row {
    flex-direction: column;
    align-items: stretch;
  }

  .search-actions {
    justify-content: stretch;
  }

  .concept-actions .btn,
  .search-actions .btn {
    flex: 1;
  }
}
</style>
