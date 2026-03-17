package com.philxin.interviewos.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.philxin.interviewos.common.BusinessException;
import com.philxin.interviewos.common.LogSanitizer;
import com.philxin.interviewos.controller.dto.training.FeedbackBandResponse;
import com.philxin.interviewos.controller.dto.training.StartTrainingSessionRequest;
import com.philxin.interviewos.controller.dto.training.SubmitSessionAnswerRequest;
import com.philxin.interviewos.controller.dto.training.TrainingFeedbackResponse;
import com.philxin.interviewos.controller.dto.training.TrainingHintResponse;
import com.philxin.interviewos.controller.dto.training.TrainingSessionDetailResponse;
import com.philxin.interviewos.controller.dto.training.TrainingSessionListResponse;
import com.philxin.interviewos.controller.dto.training.TrainingSessionStartResponse;
import com.philxin.interviewos.controller.dto.training.TrainingSessionSummaryResponse;
import com.philxin.interviewos.entity.Difficulty;
import com.philxin.interviewos.entity.FeedbackBand;
import com.philxin.interviewos.entity.Knowledge;
import com.philxin.interviewos.entity.KnowledgeStatus;
import com.philxin.interviewos.entity.QuestionType;
import com.philxin.interviewos.entity.TrainingQuestion;
import com.philxin.interviewos.entity.TrainingSession;
import com.philxin.interviewos.entity.TrainingSessionStatus;
import com.philxin.interviewos.llm.FeedbackGenerationRequest;
import com.philxin.interviewos.llm.FeedbackGenerationResult;
import com.philxin.interviewos.llm.LLMService;
import com.philxin.interviewos.repository.KnowledgeRepository;
import com.philxin.interviewos.repository.TrainingQuestionRepository;
import com.philxin.interviewos.repository.TrainingSessionRepository;
import com.philxin.interviewos.security.AuthenticatedUser;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * V2 训练主链路服务：会话启动、回答提交、历史摘要和详情查询。
 */
@Service
public class TrainingSessionService {
    private static final Logger log = LoggerFactory.getLogger(TrainingSessionService.class);
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    private final KnowledgeRepository knowledgeRepository;
    private final TrainingSessionRepository trainingSessionRepository;
    private final TrainingQuestionRepository trainingQuestionRepository;
    private final LLMService llmService;
    private final MasteryService masteryService;
    private final ObjectMapper objectMapper;

    public TrainingSessionService(
        KnowledgeRepository knowledgeRepository,
        TrainingSessionRepository trainingSessionRepository,
        TrainingQuestionRepository trainingQuestionRepository,
        LLMService llmService,
        MasteryService masteryService,
        ObjectMapper objectMapper
    ) {
        this.knowledgeRepository = knowledgeRepository;
        this.trainingSessionRepository = trainingSessionRepository;
        this.trainingQuestionRepository = trainingQuestionRepository;
        this.llmService = llmService;
        this.masteryService = masteryService;
        this.objectMapper = objectMapper;
    }

    /**
     * 启动一次新的训练会话，P0 默认生成单题。
     */
    @Transactional
    public TrainingSessionStartResponse startSession(
        AuthenticatedUser authenticatedUser,
        StartTrainingSessionRequest request
    ) {
        Knowledge knowledge = getActiveKnowledge(authenticatedUser, request.getKnowledgeId());
        QuestionType questionType = request.getQuestionType() == null ? QuestionType.FUNDAMENTAL : request.getQuestionType();
        Difficulty difficulty = request.getDifficulty() == null ? Difficulty.MEDIUM : request.getDifficulty();
        boolean hintEnabled = request.getHintEnabled() == null || request.getHintEnabled();

        String generatedQuestion = llmService.generateQuestion(
            knowledge.getTitle(),
            buildQuestionContext(knowledge, questionType, difficulty, authenticatedUser)
        );

        TrainingSession session = new TrainingSession();
        session.setUser(knowledge.getUser());
        session.setKnowledge(knowledge);
        session.setQuestionType(questionType);
        session.setDifficulty(difficulty);
        session.setHintEnabled(hintEnabled);
        session.setStatus(TrainingSessionStatus.IN_PROGRESS);
        session.setTotalQuestions(1);
        session.setAnsweredQuestions(0);
        session.setCurrentQuestionNo(1);
        session = trainingSessionRepository.save(session);

        TrainingQuestion question = new TrainingQuestion();
        question.setSession(session);
        question.setKnowledge(knowledge);
        question.setOrderNo(1);
        question.setQuestionType(questionType);
        question.setDifficulty(difficulty);
        question.setQuestionText(normalize(generatedQuestion));
        question = trainingQuestionRepository.save(question);

        log.info(
            "Training session started: sessionId={}, knowledgeId={}, questionType={}, difficulty={}, hintEnabled={}",
            session.getId(),
            knowledge.getId(),
            questionType,
            difficulty,
            hintEnabled
        );
        return buildStartResponse(session, question);
    }

    /**
     * 提交回答，更新题目反馈、session 摘要和知识点掌握度。
     */
    @Transactional
    public TrainingFeedbackResponse submitAnswer(
        AuthenticatedUser authenticatedUser,
        UUID sessionId,
        SubmitSessionAnswerRequest request
    ) {
        TrainingSession session = getSession(authenticatedUser, sessionId);
        if (session.getStatus() != TrainingSessionStatus.IN_PROGRESS) {
            throw new BusinessException(HttpStatus.CONFLICT, "Training session is not in progress");
        }

        TrainingQuestion question = trainingQuestionRepository.findByIdAndSessionId(request.getQuestionId(), sessionId)
            .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Training question not found"));
        if (question.getAnsweredAt() != null) {
            throw new BusinessException(HttpStatus.CONFLICT, "Training question already answered");
        }

        String normalizedAnswer = normalize(request.getAnswer());
        FeedbackGenerationResult feedbackResult = llmService.evaluateAnswer(
            buildFeedbackRequest(question, normalizedAnswer)
        );

        int score = resolveScore(feedbackResult);
        FeedbackBand band = masteryService.resolveBand(score);
        int masteryBefore = masteryService.normalizeScore(question.getKnowledge().getMastery() == null ? 0 : question.getKnowledge().getMastery());
        int masteryAfter = masteryService.calculateMastery(masteryBefore, score);
        List<String> missingPoints = resolveMissingPoints(feedbackResult);
        List<String> betterAnswerApproach = resolveBetterAnswerApproach(feedbackResult);
        List<String> weakTags = masteryService.resolveWeakTags(question.getKnowledge());
        String majorIssue = resolveMajorIssue(feedbackResult, score);
        String naturalExampleAnswer = normalize(feedbackResult.getNaturalExampleAnswer());

        question.getKnowledge().setMastery(masteryAfter);
        knowledgeRepository.save(question.getKnowledge());

        question.setAnswerText(normalizedAnswer);
        question.setScore(score);
        question.setFeedbackBand(band);
        question.setMajorIssue(majorIssue);
        question.setMissingPoints(writeJson(missingPoints));
        question.setBetterAnswerApproach(writeJson(betterAnswerApproach));
        question.setNaturalExampleAnswer(naturalExampleAnswer);
        question.setWeakTags(writeJson(weakTags));
        question.setMasteryBefore(masteryBefore);
        question.setMasteryAfter(masteryAfter);
        question.setAnsweredAt(LocalDateTime.now());
        trainingQuestionRepository.save(question);

        session.setAnsweredQuestions(1);
        session.setSummaryScore(score);
        session.setSummaryBand(band);
        session.setSummaryMajorIssue(truncate(majorIssue, 255));
        session.setStatus(TrainingSessionStatus.COMPLETED);
        session.setCompletedAt(LocalDateTime.now());
        trainingSessionRepository.save(session);

        log.info(
            "Training session answered: sessionId={}, questionId={}, score={}, band={}, masteryBefore={}, masteryAfter={}",
            sessionId,
            question.getId(),
            score,
            band,
            masteryBefore,
            masteryAfter
        );
        return buildFeedbackResponse(question);
    }

    /**
     * 获取当前题目的答题提示，重复请求时返回已有提示。
     */
    @Transactional
    public TrainingHintResponse getHint(AuthenticatedUser authenticatedUser, UUID sessionId, UUID questionId) {
        TrainingSession session = getSession(authenticatedUser, sessionId);
        if (!Boolean.TRUE.equals(session.getHintEnabled())) {
            throw new BusinessException(HttpStatus.CONFLICT, "Training hint is disabled");
        }
        if (session.getStatus() != TrainingSessionStatus.IN_PROGRESS) {
            throw new BusinessException(HttpStatus.CONFLICT, "Training session is not in progress");
        }

        TrainingQuestion question = getQuestion(sessionId, questionId);
        if (question.getAnsweredAt() != null) {
            throw new BusinessException(HttpStatus.CONFLICT, "Training question already answered");
        }

        String existingHint = normalize(question.getHintText());
        if (!existingHint.isEmpty()) {
            return buildHintResponse(existingHint);
        }

        String generatedHint = normalize(
            llmService.generateHint(
                question.getKnowledge().getTitle(),
                question.getKnowledge().getContent(),
                question.getQuestionText()
            )
        );
        if (generatedHint.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "LLM did not return hint content");
        }

        question.setHintText(generatedHint);
        question.setHintUsed(true);
        trainingQuestionRepository.save(question);

        log.info(
            "Training hint generated: sessionId={}, questionId={}, hintLength={}, hintFingerprint={}",
            sessionId,
            questionId,
            LogSanitizer.length(generatedHint),
            LogSanitizer.fingerprint(generatedHint)
        );
        return buildHintResponse(generatedHint);
    }

    /**
     * 查询当前用户训练历史。
     */
    @Transactional(readOnly = true)
    public TrainingSessionListResponse getSessionHistory(
        AuthenticatedUser authenticatedUser,
        Long knowledgeId,
        Integer page,
        Integer size
    ) {
        Long userId = getCurrentUserId(authenticatedUser);
        int resolvedPage = page == null || page < 1 ? 1 : page;
        int resolvedSize = size == null ? 20 : Math.min(Math.max(size, 1), 100);

        List<TrainingSession> sessions;
        if (knowledgeId == null) {
            sessions = trainingSessionRepository.findByUserIdOrderByCreatedAtDesc(userId);
        } else {
            getKnowledgeForHistory(userId, knowledgeId);
            sessions = trainingSessionRepository.findByUserIdAndKnowledgeIdOrderByCreatedAtDesc(userId, knowledgeId);
        }

        int fromIndex = Math.min((resolvedPage - 1) * resolvedSize, sessions.size());
        int toIndex = Math.min(fromIndex + resolvedSize, sessions.size());
        List<TrainingSessionSummaryResponse> items = sessions.subList(fromIndex, toIndex)
            .stream()
            .map(this::buildSummaryResponse)
            .toList();

        TrainingSessionListResponse response = new TrainingSessionListResponse();
        response.setItems(items);
        response.setPage(resolvedPage);
        response.setSize(resolvedSize);
        response.setTotal(sessions.size());
        response.setHasNext(toIndex < sessions.size());
        return response;
    }

    /**
     * 查询训练会话详情。
     */
    @Transactional(readOnly = true)
    public TrainingSessionDetailResponse getSessionDetail(AuthenticatedUser authenticatedUser, UUID sessionId) {
        TrainingSession session = getSession(authenticatedUser, sessionId);
        List<TrainingQuestion> questions = trainingQuestionRepository.findBySessionIdOrderByOrderNoAsc(sessionId);

        TrainingSessionDetailResponse response = new TrainingSessionDetailResponse();
        response.setSessionId(session.getId());
        response.setKnowledgeId(session.getKnowledge().getId());
        response.setKnowledgeTitle(session.getKnowledge().getTitle());
        response.setQuestionCount(session.getTotalQuestions());
        response.setAnsweredCount(session.getAnsweredQuestions());
        response.setSessionScore(session.getSummaryScore());
        response.setBand(FeedbackBandResponse.fromBand(session.getSummaryBand()));
        response.setMajorIssueSummary(session.getSummaryMajorIssue());
        response.setStartedAt(session.getStartedAt());
        response.setCompletedAt(session.getCompletedAt());
        response.setQuestions(questions.stream().map(this::buildQuestionDetail).toList());
        return response;
    }

    private TrainingSessionStartResponse buildStartResponse(TrainingSession session, TrainingQuestion question) {
        TrainingSessionStartResponse response = new TrainingSessionStartResponse();
        response.setSessionId(session.getId());
        response.setQuestionId(question.getId());
        response.setKnowledgeId(session.getKnowledge().getId());
        response.setKnowledgeTitle(session.getKnowledge().getTitle());
        response.setQuestion(question.getQuestionText());
        response.setQuestionType(session.getQuestionType().name());
        response.setDifficulty(session.getDifficulty().name());
        response.setHintAvailable(Boolean.TRUE.equals(session.getHintEnabled()));
        response.setSequence(new TrainingSessionStartResponse.Sequence(question.getOrderNo(), session.getTotalQuestions()));
        return response;
    }

    private TrainingSessionSummaryResponse buildSummaryResponse(TrainingSession session) {
        TrainingSessionSummaryResponse response = new TrainingSessionSummaryResponse();
        response.setSessionId(session.getId());
        response.setKnowledgeId(session.getKnowledge().getId());
        response.setKnowledgeTitle(session.getKnowledge().getTitle());
        response.setQuestionCount(session.getTotalQuestions());
        response.setAnsweredCount(session.getAnsweredQuestions());
        response.setSessionScore(session.getSummaryScore());
        response.setBand(FeedbackBandResponse.fromBand(session.getSummaryBand()));
        response.setMajorIssueSummary(session.getSummaryMajorIssue());
        response.setStartedAt(session.getStartedAt());
        response.setCompletedAt(session.getCompletedAt());
        return response;
    }

    private TrainingSessionDetailResponse.QuestionDetail buildQuestionDetail(TrainingQuestion question) {
        TrainingSessionDetailResponse.QuestionDetail detail = new TrainingSessionDetailResponse.QuestionDetail();
        detail.setQuestionId(question.getId());
        detail.setOrderNo(question.getOrderNo());
        detail.setParentQuestionId(question.getParentQuestion() == null ? null : question.getParentQuestion().getId());
        detail.setQuestionType(question.getQuestionType().name());
        detail.setDifficulty(question.getDifficulty().name());
        detail.setQuestion(question.getQuestionText());
        detail.setHintAvailable(isHintAvailable(question));
        detail.setHintText(question.getHintText());
        detail.setHintUsed(Boolean.TRUE.equals(question.getHintUsed()));
        detail.setAnswer(question.getAnswerText());
        detail.setFeedback(question.getScore() == null ? null : buildFeedbackResponse(question));
        return detail;
    }

    private TrainingHintResponse buildHintResponse(String hint) {
        TrainingHintResponse response = new TrainingHintResponse();
        response.setHint(hint);
        return response;
    }

    private TrainingFeedbackResponse buildFeedbackResponse(TrainingQuestion question) {
        TrainingFeedbackResponse response = new TrainingFeedbackResponse();
        response.setScore(question.getScore());
        response.setBand(FeedbackBandResponse.fromBand(question.getFeedbackBand()));
        response.setMajorIssue(question.getMajorIssue());
        response.setMissingPoints(readJson(question.getMissingPoints()));
        response.setBetterAnswerApproach(readJson(question.getBetterAnswerApproach()));
        response.setNaturalExampleAnswer(question.getNaturalExampleAnswer());
        response.setWeakTags(readJson(question.getWeakTags()));
        response.setMasteryBefore(question.getMasteryBefore());
        response.setMasteryAfter(question.getMasteryAfter());
        return response;
    }

    private TrainingSession getSession(AuthenticatedUser authenticatedUser, UUID sessionId) {
        return trainingSessionRepository.findByIdAndUserId(sessionId, getCurrentUserId(authenticatedUser))
            .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Training session not found"));
    }

    private TrainingQuestion getQuestion(UUID sessionId, UUID questionId) {
        return trainingQuestionRepository.findByIdAndSessionId(questionId, sessionId)
            .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Training question not found"));
    }

    private Knowledge getActiveKnowledge(AuthenticatedUser authenticatedUser, Long knowledgeId) {
        Knowledge knowledge = knowledgeRepository.findByIdAndUserId(knowledgeId, getCurrentUserId(authenticatedUser))
            .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Knowledge not found with id: " + knowledgeId));
        if (knowledge.getStatus() != KnowledgeStatus.ACTIVE) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "Knowledge not found with id: " + knowledgeId);
        }
        return knowledge;
    }

    private void getKnowledgeForHistory(Long userId, Long knowledgeId) {
        knowledgeRepository.findByIdAndUserId(knowledgeId, userId)
            .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Knowledge not found with id: " + knowledgeId));
    }

    private Long getCurrentUserId(AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null || authenticatedUser.getId() == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return authenticatedUser.getId();
    }

    private String buildQuestionContext(
        Knowledge knowledge,
        QuestionType questionType,
        Difficulty difficulty,
        AuthenticatedUser authenticatedUser
    ) {
        String role = authenticatedUser == null ? null : authenticatedUser.getTargetRole();
        return """
            targetRole: %s
            questionType: %s
            difficulty: %s
            knowledgeContent:
            %s
            """.formatted(
            role == null ? "" : role,
            questionType.name(),
            difficulty.name(),
            knowledge.getContent()
        );
    }

    private FeedbackGenerationRequest buildFeedbackRequest(TrainingQuestion question, String normalizedAnswer) {
        FeedbackGenerationRequest request = new FeedbackGenerationRequest();
        request.setQuestionText(question.getQuestionText());
        request.setQuestionType(question.getQuestionType() == null ? null : question.getQuestionType().name());
        request.setDifficulty(question.getDifficulty() == null ? null : question.getDifficulty().name());
        request.setUserAnswer(normalizedAnswer);
        request.setKnowledgeTitle(question.getKnowledge().getTitle());
        request.setKnowledgeContent(question.getKnowledge().getContent());
        request.setHintUsed(question.getHintUsed());
        return request;
    }

    private int resolveScore(FeedbackGenerationResult feedbackResult) {
        return masteryService.normalizeScore(feedbackResult.getScore() == null ? 0 : feedbackResult.getScore());
    }

    private List<String> resolveMissingPoints(FeedbackGenerationResult feedbackResult) {
        List<String> items = normalizeList(feedbackResult.getMissingPoints());
        if (!items.isEmpty()) {
            return items;
        }
        String majorIssue = normalize(feedbackResult.getMajorIssue());
        return majorIssue.isEmpty() ? List.of("关键点仍有缺失，需要补充核心原理和落地细节。") : List.of(majorIssue);
    }

    private List<String> resolveBetterAnswerApproach(FeedbackGenerationResult feedbackResult) {
        List<String> items = normalizeList(feedbackResult.getBetterAnswerApproach());
        if (!items.isEmpty()) {
            return items;
        }
        return List.of("先给出核心结论，再按原理、条件和项目例子展开。");
    }

    private String resolveMajorIssue(FeedbackGenerationResult feedbackResult, int score) {
        String majorIssue = normalize(feedbackResult.getMajorIssue());
        if (!majorIssue.isEmpty()) {
            return truncate(majorIssue, 255);
        }
        FeedbackBand band = masteryService.resolveBand(score);
        return switch (band) {
            case UNCLEAR -> "表达不够清晰，核心结论和逻辑顺序需要先稳定下来。";
            case INCOMPLETE -> "回答方向基本正确，但关键点缺失，完整度不够。";
            case BASIC -> "基础理解具备，但深度和案例支撑仍然不足。";
            case GOOD -> "回答较完整，但还可以补充更贴近项目的细节。";
            case STRONG -> "回答整体扎实，可继续用更精炼的表达提升面试表现。";
        };
    }

    private boolean isHintAvailable(TrainingQuestion question) {
        return Boolean.TRUE.equals(question.getSession().getHintEnabled())
            && !Boolean.TRUE.equals(question.getHintUsed())
            && question.getAnsweredAt() == null;
    }

    private String writeJson(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values == null ? List.of() : values);
        } catch (JsonProcessingException exception) {
            log.error("Failed to serialize training session json field", exception);
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to persist training feedback");
        }
    }

    private List<String> readJson(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(value, STRING_LIST_TYPE);
        } catch (Exception exception) {
            log.warn(
                "Failed to parse persisted training json field: length={}, fingerprint={}",
                LogSanitizer.length(value),
                LogSanitizer.fingerprint(value),
                exception
            );
            return List.of();
        }
    }

    private List<String> normalizeList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        List<String> normalized = new ArrayList<>();
        for (String value : values) {
            String item = normalize(value);
            if (!item.isEmpty()) {
                normalized.add(item);
            }
        }
        return normalized;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String truncate(String value, int maxLength) {
        String normalized = normalize(value);
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength);
    }
}
