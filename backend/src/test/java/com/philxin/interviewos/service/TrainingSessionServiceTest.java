package com.philxin.interviewos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.philxin.interviewos.common.BusinessException;
import com.philxin.interviewos.controller.dto.training.StartTrainingSessionRequest;
import com.philxin.interviewos.controller.dto.training.SubmitSessionAnswerRequest;
import com.philxin.interviewos.controller.dto.training.TrainingFeedbackResponse;
import com.philxin.interviewos.controller.dto.training.TrainingHintResponse;
import com.philxin.interviewos.controller.dto.training.TrainingSessionDetailResponse;
import com.philxin.interviewos.controller.dto.training.TrainingSessionListResponse;
import com.philxin.interviewos.controller.dto.training.TrainingSessionStartResponse;
import com.philxin.interviewos.entity.AppUser;
import com.philxin.interviewos.entity.FeedbackBand;
import com.philxin.interviewos.entity.Knowledge;
import com.philxin.interviewos.entity.KnowledgeSourceType;
import com.philxin.interviewos.entity.KnowledgeStatus;
import com.philxin.interviewos.entity.KnowledgeTag;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class TrainingSessionServiceTest {

    @Mock
    private KnowledgeRepository knowledgeRepository;

    @Mock
    private TrainingSessionRepository trainingSessionRepository;

    @Mock
    private TrainingQuestionRepository trainingQuestionRepository;

    @Mock
    private LLMService llmService;

    private TrainingSessionService trainingSessionService;

    @BeforeEach
    void setUp() {
        trainingSessionService = new TrainingSessionService(
            knowledgeRepository,
            trainingSessionRepository,
            trainingQuestionRepository,
            llmService,
            new MasteryService(),
            new ObjectMapper()
        );
    }

    @Test
    void startSessionCreatesMultiQuestionByAutoPlan() {
        Knowledge knowledge = buildKnowledge(1L, 40);
        when(knowledgeRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(knowledge));
        when(trainingSessionRepository.findByUserIdAndKnowledgeIdOrderByCreatedAtDesc(1L, 1L)).thenReturn(List.of());
        when(llmService.generateQuestion(anyString(), anyString()))
            .thenReturn("问题一", "问题二", "问题三", "问题四");
        when(trainingSessionRepository.save(any(TrainingSession.class))).thenAnswer(invocation -> {
            TrainingSession session = invocation.getArgument(0);
            session.setId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
            return session;
        });
        when(trainingQuestionRepository.save(any(TrainingQuestion.class))).thenAnswer(invocation -> {
            TrainingQuestion question = invocation.getArgument(0);
            question.setId(UUID.fromString(String.format("22222222-2222-2222-2222-%012d", question.getOrderNo())));
            return question;
        });

        StartTrainingSessionRequest request = new StartTrainingSessionRequest();
        request.setKnowledgeId(1L);

        TrainingSessionStartResponse response = trainingSessionService.startSession(authenticatedUser(), request);

        assertEquals(UUID.fromString("11111111-1111-1111-1111-111111111111"), response.getSessionId());
        assertEquals(UUID.fromString("22222222-2222-2222-2222-000000000001"), response.getQuestionId());
        assertEquals("FUNDAMENTAL", response.getQuestionType());
        assertEquals("MEDIUM", response.getDifficulty());
        assertEquals(1, response.getSequence().getCurrent());
        assertEquals(4, response.getSequence().getTotal());
        assertEquals("问题一", response.getQuestion());
    }

    @Test
    void submitAnswerUpdatesQuestionSessionAndMastery() {
        Knowledge knowledge = buildKnowledge(1L, 50);
        TrainingSession session = buildSession(knowledge);
        TrainingQuestion question = buildQuestion(session, knowledge);
        when(trainingSessionRepository.findByIdAndUserId(session.getId(), 1L)).thenReturn(Optional.of(session));
        when(trainingQuestionRepository.findByIdAndSessionId(question.getId(), session.getId())).thenReturn(Optional.of(question));
        when(knowledgeRepository.save(any(Knowledge.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(trainingQuestionRepository.save(any(TrainingQuestion.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(trainingQuestionRepository.findBySessionIdOrderByOrderNoAsc(session.getId())).thenReturn(List.of(question));
        when(trainingSessionRepository.save(any(TrainingSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FeedbackGenerationResult feedbackResult = new FeedbackGenerationResult();
        feedbackResult.setScore(58);
        feedbackResult.setMajorIssue("关键点缺失");
        feedbackResult.setMissingPoints(List.of("先讲入口"));
        feedbackResult.setBetterAnswerApproach(List.of("再讲条件装配"));
        feedbackResult.setNaturalExampleAnswer("示例回答");
        when(llmService.evaluateAnswer(any(FeedbackGenerationRequest.class))).thenReturn(feedbackResult);

        SubmitSessionAnswerRequest request = new SubmitSessionAnswerRequest();
        request.setQuestionId(question.getId());
        request.setAnswer(" 回答 ");

        TrainingFeedbackResponse response = trainingSessionService.submitAnswer(authenticatedUser(), session.getId(), request);

        assertEquals(58, response.getScore());
        assertEquals("BASIC", response.getBand().getCode());
        assertEquals(52, response.getMasteryAfter());
        assertEquals(List.of("spring", "backend"), response.getWeakTags());
        assertEquals(TrainingSessionStatus.COMPLETED, session.getStatus());
        assertEquals(58, session.getSummaryScore());
        assertEquals(FeedbackBand.BASIC, session.getSummaryBand());
        assertEquals(52, knowledge.getMastery());

        ArgumentCaptor<TrainingQuestion> questionCaptor = ArgumentCaptor.forClass(TrainingQuestion.class);
        verify(trainingQuestionRepository).save(questionCaptor.capture());
        TrainingQuestion savedQuestion = questionCaptor.getValue();
        assertEquals("回答", savedQuestion.getAnswerText());
        assertEquals(58, savedQuestion.getScore());

        ArgumentCaptor<FeedbackGenerationRequest> feedbackRequestCaptor = ArgumentCaptor.forClass(
            FeedbackGenerationRequest.class
        );
        verify(llmService).evaluateAnswer(feedbackRequestCaptor.capture());
        FeedbackGenerationRequest feedbackRequest = feedbackRequestCaptor.getValue();
        assertEquals("原问题", feedbackRequest.getQuestionText());
        assertEquals("PROJECT", feedbackRequest.getQuestionType());
        assertEquals("MEDIUM", feedbackRequest.getDifficulty());
        assertEquals("回答", feedbackRequest.getUserAnswer());
        assertEquals("Spring", feedbackRequest.getKnowledgeTitle());
        assertEquals("content", feedbackRequest.getKnowledgeContent());
    }

    @Test
    void submitAnswerKeepsSessionInProgressBeforeLastQuestion() {
        Knowledge knowledge = buildKnowledge(1L, 55);
        TrainingSession session = buildSession(knowledge, 3, 0, 1);
        TrainingQuestion question = buildQuestion(
            session,
            knowledge,
            UUID.fromString("22222222-2222-2222-2222-222222222221"),
            1
        );
        when(trainingSessionRepository.findByIdAndUserId(session.getId(), 1L)).thenReturn(Optional.of(session));
        when(trainingQuestionRepository.findByIdAndSessionId(question.getId(), session.getId())).thenReturn(Optional.of(question));
        when(knowledgeRepository.save(any(Knowledge.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(trainingQuestionRepository.save(any(TrainingQuestion.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(trainingSessionRepository.save(any(TrainingSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FeedbackGenerationResult feedbackResult = new FeedbackGenerationResult();
        feedbackResult.setScore(66);
        feedbackResult.setMajorIssue("回答细节不足");
        feedbackResult.setMissingPoints(List.of("补充案例"));
        feedbackResult.setBetterAnswerApproach(List.of("先结论后拆解"));
        feedbackResult.setNaturalExampleAnswer("示例");
        when(llmService.evaluateAnswer(any(FeedbackGenerationRequest.class))).thenReturn(feedbackResult);

        SubmitSessionAnswerRequest request = new SubmitSessionAnswerRequest();
        request.setQuestionId(question.getId());
        request.setAnswer("回答");

        TrainingFeedbackResponse response = trainingSessionService.submitAnswer(authenticatedUser(), session.getId(), request);

        assertEquals(66, response.getScore());
        assertEquals(TrainingSessionStatus.IN_PROGRESS, session.getStatus());
        assertEquals(1, session.getAnsweredQuestions());
        assertEquals(2, session.getCurrentQuestionNo());
        assertNull(session.getSummaryScore());
    }

    @Test
    void submitAnswerCompletesSessionWithAggregatedSummaryWhenLastQuestion() {
        Knowledge knowledge = buildKnowledge(1L, 60);
        TrainingSession session = buildSession(knowledge, 3, 2, 3);
        TrainingQuestion firstQuestion = buildQuestion(
            session,
            knowledge,
            UUID.fromString("22222222-2222-2222-2222-222222222221"),
            1
        );
        firstQuestion.setScore(50);
        firstQuestion.setMajorIssue("第一题问题");

        TrainingQuestion secondQuestion = buildQuestion(
            session,
            knowledge,
            UUID.fromString("22222222-2222-2222-2222-222222222222"),
            2
        );
        secondQuestion.setScore(70);
        secondQuestion.setMajorIssue("第二题问题");

        TrainingQuestion thirdQuestion = buildQuestion(
            session,
            knowledge,
            UUID.fromString("22222222-2222-2222-2222-222222222223"),
            3
        );

        when(trainingSessionRepository.findByIdAndUserId(session.getId(), 1L)).thenReturn(Optional.of(session));
        when(trainingQuestionRepository.findByIdAndSessionId(thirdQuestion.getId(), session.getId()))
            .thenReturn(Optional.of(thirdQuestion));
        when(knowledgeRepository.save(any(Knowledge.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(trainingQuestionRepository.save(any(TrainingQuestion.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(trainingQuestionRepository.findBySessionIdOrderByOrderNoAsc(session.getId()))
            .thenReturn(List.of(firstQuestion, secondQuestion, thirdQuestion));
        when(trainingSessionRepository.save(any(TrainingSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FeedbackGenerationResult feedbackResult = new FeedbackGenerationResult();
        feedbackResult.setScore(80);
        feedbackResult.setMajorIssue("第三题问题");
        feedbackResult.setMissingPoints(List.of("补充容量边界"));
        feedbackResult.setBetterAnswerApproach(List.of("先指标后方案"));
        feedbackResult.setNaturalExampleAnswer("示例");
        when(llmService.evaluateAnswer(any(FeedbackGenerationRequest.class))).thenReturn(feedbackResult);

        SubmitSessionAnswerRequest request = new SubmitSessionAnswerRequest();
        request.setQuestionId(thirdQuestion.getId());
        request.setAnswer("最终回答");

        TrainingFeedbackResponse response = trainingSessionService.submitAnswer(authenticatedUser(), session.getId(), request);

        assertEquals(80, response.getScore());
        assertEquals(TrainingSessionStatus.COMPLETED, session.getStatus());
        assertEquals(3, session.getAnsweredQuestions());
        assertEquals(3, session.getCurrentQuestionNo());
        assertEquals(67, session.getSummaryScore());
        assertEquals(FeedbackBand.BASIC, session.getSummaryBand());
        assertEquals("第一题问题；第二题问题", session.getSummaryMajorIssue());
        assertNotNull(session.getCompletedAt());
    }

    @Test
    void submitAnswerRejectsAnsweredQuestion() {
        Knowledge knowledge = buildKnowledge(1L, 50);
        TrainingSession session = buildSession(knowledge);
        TrainingQuestion question = buildQuestion(session, knowledge);
        question.setAnsweredAt(LocalDateTime.now());
        when(trainingSessionRepository.findByIdAndUserId(session.getId(), 1L)).thenReturn(Optional.of(session));
        when(trainingQuestionRepository.findByIdAndSessionId(question.getId(), session.getId())).thenReturn(Optional.of(question));

        SubmitSessionAnswerRequest request = new SubmitSessionAnswerRequest();
        request.setQuestionId(question.getId());
        request.setAnswer("回答");

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> trainingSessionService.submitAnswer(authenticatedUser(), session.getId(), request)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertEquals("Training question already answered", exception.getMessage());
    }

    @Test
    void getHintGeneratesAndPersistsHint() {
        Knowledge knowledge = buildKnowledge(1L, 50);
        TrainingSession session = buildSession(knowledge);
        TrainingQuestion question = buildQuestion(session, knowledge);
        when(trainingSessionRepository.findByIdAndUserId(session.getId(), 1L)).thenReturn(Optional.of(session));
        when(trainingQuestionRepository.findByIdAndSessionId(question.getId(), session.getId())).thenReturn(Optional.of(question));
        when(trainingQuestionRepository.save(any(TrainingQuestion.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(llmService.generateHint(anyString(), anyString(), anyString()))
            .thenReturn("可以先讲入口，再讲条件装配，最后补项目例子。");

        TrainingHintResponse response = trainingSessionService.getHint(authenticatedUser(), session.getId(), question.getId());

        assertEquals("可以先讲入口，再讲条件装配，最后补项目例子。", response.getHint());
        assertEquals(true, question.getHintUsed());
        assertEquals("可以先讲入口，再讲条件装配，最后补项目例子。", question.getHintText());
        verify(llmService).generateHint("Spring", "content", "原问题");
    }

    @Test
    void getHintReturnsExistingHintWithoutCallingLlm() {
        Knowledge knowledge = buildKnowledge(1L, 50);
        TrainingSession session = buildSession(knowledge);
        TrainingQuestion question = buildQuestion(session, knowledge);
        question.setHintUsed(true);
        question.setHintText("先讲自动配置入口，再讲触发条件。");
        when(trainingSessionRepository.findByIdAndUserId(session.getId(), 1L)).thenReturn(Optional.of(session));
        when(trainingQuestionRepository.findByIdAndSessionId(question.getId(), session.getId())).thenReturn(Optional.of(question));

        TrainingHintResponse response = trainingSessionService.getHint(authenticatedUser(), session.getId(), question.getId());

        assertEquals("先讲自动配置入口，再讲触发条件。", response.getHint());
    }

    @Test
    void getSessionHistoryAppliesKnowledgeFilterAndPagination() {
        Knowledge knowledge = buildKnowledge(1L, 50);
        TrainingSession session1 = buildSession(knowledge);
        TrainingSession session2 = buildSession(knowledge);
        session2.setId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
        when(knowledgeRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(knowledge));
        when(trainingSessionRepository.findByUserIdAndKnowledgeIdOrderByCreatedAtDesc(1L, 1L))
            .thenReturn(List.of(session1, session2));

        TrainingSessionListResponse response = trainingSessionService.getSessionHistory(authenticatedUser(), 1L, 1, 1);

        assertEquals(1, response.getItems().size());
        assertEquals(2, response.getTotal());
        assertEquals(1, response.getPage());
        assertEquals(1, response.getSize());
        assertEquals(true, response.isHasNext());
    }

    @Test
    void getSessionDetailThrows404WhenSessionNotOwned() {
        UUID sessionId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        when(trainingSessionRepository.findByIdAndUserId(sessionId, 1L)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> trainingSessionService.getSessionDetail(authenticatedUser(), sessionId)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    void getSessionDetailReturnsQuestionsAndFeedback() {
        Knowledge knowledge = buildKnowledge(1L, 50);
        TrainingSession session = buildSession(knowledge);
        session.setStatus(TrainingSessionStatus.COMPLETED);
        session.setSummaryScore(70);
        session.setSummaryBand(FeedbackBand.GOOD);
        session.setSummaryMajorIssue("细节还可加强");

        TrainingQuestion question = buildQuestion(session, knowledge);
        question.setScore(70);
        question.setFeedbackBand(FeedbackBand.GOOD);
        question.setMajorIssue("细节还可加强");
        question.setMissingPoints("[\"补充项目例子\"]");
        question.setBetterAnswerApproach("[\"先讲结论再讲例子\"]");
        question.setWeakTags("[\"spring\",\"backend\"]");
        question.setNaturalExampleAnswer("示例");
        question.setMasteryBefore(50);
        question.setMasteryAfter(56);
        question.setAnswerText("回答");

        when(trainingSessionRepository.findByIdAndUserId(session.getId(), 1L)).thenReturn(Optional.of(session));
        when(trainingQuestionRepository.findBySessionIdOrderByOrderNoAsc(session.getId())).thenReturn(List.of(question));

        TrainingSessionDetailResponse response = trainingSessionService.getSessionDetail(authenticatedUser(), session.getId());

        assertEquals(session.getId(), response.getSessionId());
        assertEquals(1, response.getQuestions().size());
        assertEquals(true, response.getQuestions().get(0).isHintAvailable());
        assertEquals(null, response.getQuestions().get(0).getHintText());
        assertEquals("GOOD", response.getQuestions().get(0).getFeedback().getBand().getCode());
        assertEquals("回答", response.getQuestions().get(0).getAnswer());
    }

    private AuthenticatedUser authenticatedUser() {
        return AuthenticatedUser.fromEntity(buildUser());
    }

    private AppUser buildUser() {
        AppUser user = new AppUser();
        user.setId(1L);
        user.setEmail("user@example.com");
        user.setDisplayName("philxin");
        user.setPasswordHash("hash");
        return user;
    }

    private Knowledge buildKnowledge(Long id, Integer mastery) {
        Knowledge knowledge = new Knowledge();
        knowledge.setId(id);
        knowledge.setUser(buildUser());
        knowledge.setTitle("Spring");
        knowledge.setContent("content");
        knowledge.setMastery(mastery);
        knowledge.setSourceType(KnowledgeSourceType.MANUAL);
        knowledge.setStatus(KnowledgeStatus.ACTIVE);
        knowledge.setCreatedAt(LocalDateTime.of(2026, 3, 8, 0, 0));
        knowledge.setUpdatedAt(LocalDateTime.of(2026, 3, 8, 0, 0));

        KnowledgeTag tag1 = new KnowledgeTag();
        tag1.setKnowledge(knowledge);
        tag1.setTag("spring");
        knowledge.getTags().add(tag1);
        KnowledgeTag tag2 = new KnowledgeTag();
        tag2.setKnowledge(knowledge);
        tag2.setTag("backend");
        knowledge.getTags().add(tag2);
        return knowledge;
    }

    private TrainingSession buildSession(Knowledge knowledge) {
        TrainingSession session = new TrainingSession();
        session.setId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        session.setUser(buildUser());
        session.setKnowledge(knowledge);
        session.setQuestionType(QuestionType.PROJECT);
        session.setDifficulty(com.philxin.interviewos.entity.Difficulty.MEDIUM);
        session.setHintEnabled(true);
        session.setStatus(TrainingSessionStatus.IN_PROGRESS);
        session.setTotalQuestions(1);
        session.setAnsweredQuestions(0);
        session.setCurrentQuestionNo(1);
        session.setStartedAt(LocalDateTime.of(2026, 3, 8, 0, 0));
        session.setCreatedAt(LocalDateTime.of(2026, 3, 8, 0, 0));
        return session;
    }

    private TrainingSession buildSession(Knowledge knowledge, int totalQuestions, int answeredQuestions, int currentQuestionNo) {
        TrainingSession session = buildSession(knowledge);
        session.setTotalQuestions(totalQuestions);
        session.setAnsweredQuestions(answeredQuestions);
        session.setCurrentQuestionNo(currentQuestionNo);
        return session;
    }

    private TrainingQuestion buildQuestion(TrainingSession session, Knowledge knowledge) {
        return buildQuestion(
            session,
            knowledge,
            UUID.fromString("22222222-2222-2222-2222-222222222222"),
            1
        );
    }

    private TrainingQuestion buildQuestion(TrainingSession session, Knowledge knowledge, UUID questionId, int orderNo) {
        TrainingQuestion question = new TrainingQuestion();
        question.setId(questionId);
        question.setSession(session);
        question.setKnowledge(knowledge);
        question.setOrderNo(orderNo);
        question.setQuestionType(QuestionType.PROJECT);
        question.setDifficulty(com.philxin.interviewos.entity.Difficulty.MEDIUM);
        question.setQuestionText("原问题");
        question.setCreatedAt(LocalDateTime.of(2026, 3, 8, 0, 0));
        return question;
    }
}
