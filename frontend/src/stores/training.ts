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

  function primeLatestDetailFromFeedback(feedback: TrainingFeedback) {
    if (!currentSession.value) {
      return
    }

    latestDetail.value = {
      sessionId: currentSession.value.sessionId,
      knowledgeId: currentSession.value.knowledgeId,
      knowledgeTitle: currentSession.value.knowledgeTitle,
      questionCount: currentSession.value.sequence.total,
      answeredCount: currentSession.value.sequence.current,
      sessionScore: feedback.score,
      band: feedback.band,
      majorIssueSummary: feedback.majorIssue,
      startedAt: latestDetail.value?.startedAt ?? new Date().toISOString(),
      completedAt: new Date().toISOString(),
      questions: [
        {
          questionId: currentSession.value.questionId,
          orderNo: currentSession.value.sequence.current,
          parentQuestionId: null,
          questionType: currentSession.value.questionType,
          difficulty: currentSession.value.difficulty,
          question: currentSession.value.question,
          hintAvailable: false,
          hintText: currentHint.value || null,
          hintUsed: Boolean(currentHint.value),
          answer: currentAnswer.value,
          feedback,
        },
      ],
    }
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
    primeLatestDetailFromFeedback,
    clearSession,
  }
})
