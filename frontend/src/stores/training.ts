import { defineStore } from 'pinia'
import { ref } from 'vue'
import type {
  TrainingFeedback,
  TrainingSessionDetail,
  TrainingSessionStartResponse,
} from '../types'

export const useTrainingStore = defineStore('training', () => {
  const currentSession = ref<TrainingSessionStartResponse | null>(null)
  const currentAnswer = ref('')
  const currentHint = ref('')
  const latestFeedback = ref<TrainingFeedback | null>(null)
  const latestDetail = ref<TrainingSessionDetail | null>(null)

  function setCurrentSession(session: TrainingSessionStartResponse) {
    currentSession.value = session
  }

  function setCurrentAnswer(answer: string) {
    currentAnswer.value = answer
  }

  function setCurrentHint(hint: string) {
    currentHint.value = hint
  }

  function setLatestFeedback(feedback: TrainingFeedback | null) {
    latestFeedback.value = feedback
  }

  function setLatestDetail(detail: TrainingSessionDetail | null) {
    latestDetail.value = detail
  }

  function clearSession() {
    currentSession.value = null
    currentAnswer.value = ''
    currentHint.value = ''
    latestFeedback.value = null
    latestDetail.value = null
  }

  return {
    currentSession,
    currentAnswer,
    currentHint,
    latestFeedback,
    latestDetail,
    setCurrentSession,
    setCurrentAnswer,
    setCurrentHint,
    setLatestFeedback,
    setLatestDetail,
    clearSession,
  }
})
