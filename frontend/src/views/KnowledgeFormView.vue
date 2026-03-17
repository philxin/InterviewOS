<template>
  <section class="form-page">
    <header class="page-header">
      <h1>{{ isEditMode ? '编辑知识点' : '新建知识点' }}</h1>
      <p>标题长度 1-200，内容必填，标签用逗号分隔。</p>
    </header>

    <div v-if="loading" class="card loading-card">正在加载知识点...</div>

    <form v-else class="card form-card" @submit.prevent="onSubmit">
      <label class="field">
        <span>标题</span>
        <input v-model.trim="form.title" maxlength="200" placeholder="例如：Spring Boot 自动配置" />
      </label>

      <label class="field">
        <span>内容</span>
        <textarea
          v-model.trim="form.content"
          rows="10"
          placeholder="请输入该知识点的关键原理、实现细节或面试要点"
        />
      </label>

      <label class="field">
        <span>标签</span>
        <input
          v-model.trim="form.tagsInput"
          maxlength="500"
          placeholder="例如：spring, backend, autoconfiguration"
        />
      </label>

      <div v-if="normalizedTags.length > 0" class="tag-preview">
        <span v-for="tag in normalizedTags" :key="tag" class="tag-chip">{{ tag }}</span>
      </div>

      <p v-if="errorMessage" class="error">{{ errorMessage }}</p>

      <div class="actions">
        <button class="btn" type="button" :disabled="saving" @click="goBack">取消</button>
        <button class="btn btn-primary" type="submit" :disabled="saving">
          {{ saving ? '提交中...' : isEditMode ? '保存修改' : '创建知识点' }}
        </button>
      </div>
    </form>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { knowledgeAPI } from '../api'

const route = useRoute()
const router = useRouter()

const saving = ref(false)
const loading = ref(false)
const errorMessage = ref('')

const form = reactive({
  title: '',
  content: '',
  tagsInput: '',
})

const knowledgeId = computed(() => {
  const value = Number(route.params.id)
  return Number.isFinite(value) && value > 0 ? value : null
})

const isEditMode = computed(() => knowledgeId.value !== null)

async function loadKnowledge() {
  if (!isEditMode.value || !knowledgeId.value) {
    return
  }
  loading.value = true
  errorMessage.value = ''
  try {
    const data = await knowledgeAPI.getById(knowledgeId.value)
    form.title = data.title
    form.content = data.content
    form.tagsInput = data.tags.join(', ')
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '获取知识点失败'
  } finally {
    loading.value = false
  }
}

function validateForm() {
  if (!form.title) {
    errorMessage.value = '标题不能为空'
    return false
  }
  if (form.title.length > 200) {
    errorMessage.value = '标题不能超过 200 个字符'
    return false
  }
  if (!form.content) {
    errorMessage.value = '内容不能为空'
    return false
  }
  const invalidTag = normalizedTags.value.find((tag) => tag.length > 50)
  if (invalidTag) {
    errorMessage.value = `标签长度不能超过 50 个字符：${invalidTag}`
    return false
  }
  return true
}

const normalizedTags = computed(() => {
  const rawItems = form.tagsInput
    .split(',')
    .map((item) => item.trim().toLowerCase())
    .filter(Boolean)
  return Array.from(new Set(rawItems))
})

async function onSubmit() {
  if (!validateForm()) {
    return
  }

  saving.value = true
  errorMessage.value = ''
  try {
    if (isEditMode.value && knowledgeId.value) {
      await knowledgeAPI.update(knowledgeId.value, {
        title: form.title,
        content: form.content,
        tags: normalizedTags.value,
      })
    } else {
      await knowledgeAPI.create({
        title: form.title,
        content: form.content,
        tags: normalizedTags.value,
      })
    }
    router.push('/')
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '保存失败'
  } finally {
    saving.value = false
  }
}

function goBack() {
  router.push('/')
}

onMounted(loadKnowledge)
</script>

<style scoped>
.form-page {
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

.form-card {
  padding: 16px;
  display: grid;
  gap: 14px;
}

.loading-card {
  padding: 16px;
}

.field {
  display: grid;
  gap: 6px;
}

.field span {
  font-size: 14px;
  color: #334155;
  font-weight: 600;
}

.field input,
.field textarea {
  width: 100%;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  padding: 10px;
  background: #fff;
}

.field textarea {
  resize: vertical;
}

.error {
  margin: 0;
  color: #b91c1c;
}

.tag-preview {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag-chip {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 4px 10px;
  border-radius: 999px;
  background: #ecfeff;
  color: #155e75;
  font-size: 13px;
  font-weight: 700;
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>
