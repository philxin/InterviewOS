/**
 * 后端统一响应包装：成功 code=0，错误 code=4xx/5xx。
 */
export interface ApiResult<T> {
  code: number
  message: string
  data: T
}

export type TargetRole =
  | 'JAVA_BACKEND'
  | 'FRONTEND'
  | 'FULLSTACK'
  | 'DEVOPS'
  | 'DATA_ENGINEER'

export interface AuthUser {
  id: number
  email: string
  displayName: string
  targetRole: TargetRole | null
}

export interface AuthResponse {
  token: string
  tokenType: 'Bearer' | string
  expiresIn: number
  user: AuthUser
}

export interface LoginRequest {
  email: string
  password: string
}

export interface RegisterRequest {
  invitationCode: string
  email: string
  password: string
  displayName: string
}

export interface CreateRegistrationInvitationRequest {
  email: string
}

export interface RegistrationInvitation {
  invitationCode: string
  inviteeEmail: string
  registrationPath: string
  expiresAt: string
}

export interface PublicRegistrationInvitation {
  invitationCode: string
  inviteeEmail: string
  expiresAt: string
}

export interface UpdateOnboardingRequest {
  targetRole: TargetRole
}

export interface UserOnboardingResponse {
  id: number
  targetRole: TargetRole
}

/**
 * 知识点实体（对应 /knowledge）。
 */
export interface Knowledge {
  id: number
  title: string
  content: string
  mastery: number
  tags: string[]
  sourceType: 'MANUAL' | 'BATCH_IMPORT' | 'FILE_IMPORT' | 'ROLE_GENERATED' | null
  status: 'ACTIVE' | 'ARCHIVED' | null
  createdAt: string
  updatedAt: string
  archivedAt: string | null
}

export interface CreateKnowledgeRequest {
  title: string
  content: string
  tags?: string[]
}

export interface UpdateKnowledgeRequest {
  title: string
  content: string
  tags?: string[]
}

export interface BatchImportKnowledgeItemRequest {
  title: string
  content: string
  tags?: string[]
}

export interface BatchImportKnowledgeRequest {
  items: BatchImportKnowledgeItemRequest[]
}

export interface BatchImportKnowledgeFailedItem {
  index: number
  title: string
  reason: string
}

export interface BatchImportKnowledgeResponse {
  createdCount: number
  failedCount: number
  failedItems: BatchImportKnowledgeFailedItem[]
}

export type KnowledgeFileImportStatus =
  | 'PENDING'
  | 'PROCESSING'
  | 'CHUNKING'
  | 'EMBEDDING'
  | 'SUCCESS'
  | 'PARTIAL'
  | 'FAILED'

export interface KnowledgeFileImportStartResponse {
  importId: string
  documentId: string | null
  status: KnowledgeFileImportStatus
  fileName: string
  fileSize: number
  totalChunks: number
  embeddedChunks: number
  failedChunks: number
}

export interface KnowledgeFileImportResponse {
  importId: string
  documentId: string | null
  fileName: string
  contentType: string
  fileSize: number
  status: KnowledgeFileImportStatus
  defaultTags: string[]
  createdCount: number
  totalChunks: number
  embeddedChunks: number
  failedChunks: number
  embeddingModel: string | null
  embeddingDim: number | null
  failureReason: string | null
  createdAt: string
  updatedAt: string
  completedAt: string | null
}

export type KnowledgeDocumentStatus = 'PROCESSING' | 'ACTIVE' | 'ARCHIVED' | 'FAILED'

export interface KnowledgeDocument {
  documentId: string
  importId: string | null
  title: string
  originalFileName: string
  contentType: string
  status: KnowledgeDocumentStatus
  totalChunks: number
  activeChunks: number
  embeddingModel: string | null
  embeddingDim: number | null
  indexedAt: string | null
  createdAt: string
  updatedAt: string
}

export type KnowledgeConceptStatus = 'CANDIDATE' | 'ACCEPTED' | 'REJECTED'

export interface KnowledgeConcept {
  conceptId: number
  documentId: string
  name: string
  summary: string | null
  aliases: string[]
  supportingChunkIds: number[]
  confidence: number | null
  status: KnowledgeConceptStatus
  acceptedKnowledgeId: number | null
  createdAt: string
  updatedAt: string
}

export interface AcceptKnowledgeConceptRequest {
  mergeKnowledgeId?: number
  title?: string
  content?: string
  tags?: string[]
}

export interface RagSearchItem {
  chunkId: number
  documentId: string
  documentTitle: string
  score: number
  snippet: string
  pageFrom: number | null
  pageTo: number | null
  startOffset: number | null
  endOffset: number | null
}

export interface RagSearchResponse {
  query: string
  documentId: string | null
  topK: number
  hitCount: number
  degraded: boolean
  items: RagSearchItem[]
}

export interface DashboardWeakKnowledgeItem {
  knowledgeId: number
  title: string
  mastery: number
  tags: string[]
}

export interface DashboardRecentTrainingItem {
  sessionId: string
  knowledgeId: number
  knowledgeTitle: string
  sessionScore: number
  band: FeedbackBand | null
  completedAt: string
}

export interface DashboardProgressSummary {
  trainedCountLast7Days: number
  averageScoreLast7Days: number
  improvedKnowledgeCount: number
}

export interface DashboardOverview {
  weakKnowledgeItems: DashboardWeakKnowledgeItem[]
  recentTrainings: DashboardRecentTrainingItem[]
  progressSummary: DashboardProgressSummary
}

export interface DashboardReviewReminderItem {
  knowledgeId: number
  knowledgeTitle: string
  reviewWeight: number
  reason: string
  suggestedQuestionType: QuestionType
  suggestedDifficulty: Difficulty
  lastTrainedAt: string | null
  tags: string[]
}

export interface DashboardReviewReminder {
  items: DashboardReviewReminderItem[]
  generatedAt: string
}

export interface TrainingRecommendationItem {
  knowledgeId: number
  questionType: QuestionType
  difficulty: Difficulty
}

export interface TrainingRecommendation {
  packageId: string
  title: string
  items: TrainingRecommendationItem[]
}

export interface PagedResult<T> {
  items: T[]
  page: number
  size: number
  total: number
  hasNext: boolean
}

export interface FeedbackBand {
  code: 'UNCLEAR' | 'INCOMPLETE' | 'BASIC' | 'GOOD' | 'STRONG'
  label: string
  description?: string
}

export type RetrievalMode = 'RAG' | 'FALLBACK'

export type TrainingReferenceUsageType = 'QUESTION' | 'HINT' | 'FEEDBACK'

export interface TrainingReference {
  usageType: TrainingReferenceUsageType
  chunkId: number | null
  documentId: string | null
  documentTitle: string
  excerpt: string
  similarityScore: number | null
  pageFrom: number | null
  pageTo: number | null
  startOffset: number | null
  endOffset: number | null
}

export interface TrainingFeedback {
  score: number
  band: FeedbackBand
  retrievalMode: RetrievalMode
  references: TrainingReference[]
  majorIssue: string
  missingPoints: string[]
  betterAnswerApproach: string[]
  naturalExampleAnswer: string
  weakTags: string[]
  masteryBefore: number
  masteryAfter: number
}

export type QuestionType = 'FUNDAMENTAL' | 'PROJECT' | 'SCENARIO'
export type Difficulty = 'EASY' | 'MEDIUM' | 'HARD'

export interface StartTrainingSessionRequest {
  knowledgeId: number
  questionType?: QuestionType
  difficulty?: Difficulty
  hintEnabled?: boolean
}

export interface TrainingSessionStartResponse {
  sessionId: string
  questionId: string
  knowledgeId: number
  knowledgeTitle: string
  question: string
  questionType: QuestionType
  difficulty: Difficulty
  hintAvailable: boolean
  retrievalMode: RetrievalMode
  references: TrainingReference[]
  sequence: {
    current: number
    total: number
  }
}

export interface SubmitSessionAnswerRequest {
  questionId: string
  answer: string
}

export interface TrainingHintResponse {
  hint: string
  retrievalMode: RetrievalMode
  references: TrainingReference[]
}

export interface TrainingSessionSummary {
  sessionId: string
  knowledgeId: number
  knowledgeTitle: string
  questionCount: number
  answeredCount: number
  sessionScore: number
  band: FeedbackBand | null
  majorIssueSummary: string
  startedAt: string
  completedAt: string | null
}

export interface TrainingSessionQuestionDetail {
  questionId: string
  orderNo: number
  parentQuestionId: string | null
  questionType: QuestionType
  difficulty: Difficulty
  question: string
  hintAvailable: boolean
  hintText: string | null
  hintUsed: boolean
  answer: string | null
  feedback: TrainingFeedback | null
  questionReferences: TrainingReference[]
  hintReferences: TrainingReference[]
  feedbackReferences: TrainingReference[]
}

export interface TrainingSessionDetail {
  sessionId: string
  knowledgeId: number
  knowledgeTitle: string
  questionCount: number
  answeredCount: number
  sessionScore: number
  band: FeedbackBand | null
  majorIssueSummary: string
  startedAt: string
  completedAt: string | null
  questions: TrainingSessionQuestionDetail[]
}

/**
 * 提交评分结果（对应 /training/submit）。
 */
export interface EvaluationResult {
  accuracy: number
  depth: number
  clarity: number
  overall: number
  strengths: string
  weaknesses: string
  suggestions: string[]
  exampleAnswer: string
}

export interface StartTrainingRequest {
  knowledgeId: number
}

export interface StartTrainingResponse {
  question: string
}

export interface SubmitTrainingRequest {
  knowledgeId: number
  question: string
  answer: string
}

/**
 * 训练历史记录（对应 /training/history*）。
 */
export interface TrainingRecord {
  id: number
  knowledgeId: number
  question: string
  answer: string
  accuracy: number
  depth: number
  clarity: number
  overall: number
  strengths: string
  weaknesses: string
  suggestions: string[]
  exampleAnswer: string
  createdAt: string
}
