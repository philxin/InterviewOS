import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { EvaluationResult } from '../types'

export const useTrainingStore = defineStore('training', () => {
  const knowledgeId = ref<number | null>(null)
  const knowledgeTitle = ref('')
  const question = ref('')
  const answer = ref('')
  const evaluation = ref<EvaluationResult | null>(null)

  function setSession(payload: {
    knowledgeId: number
    knowledgeTitle: string
    question: string
    answer: string
    evaluation: EvaluationResult
  }) {
    knowledgeId.value = payload.knowledgeId
    knowledgeTitle.value = payload.knowledgeTitle
    question.value = payload.question
    answer.value = payload.answer
    evaluation.value = payload.evaluation
  }

  function clearSession() {
    knowledgeId.value = null
    knowledgeTitle.value = ''
    question.value = ''
    answer.value = ''
    evaluation.value = null
  }

  return {
    knowledgeId,
    knowledgeTitle,
    question,
    answer,
    evaluation,
    setSession,
    clearSession,
  }
})
