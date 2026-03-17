package com.philxin.interviewos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.philxin.interviewos.common.BusinessException;
import com.philxin.interviewos.controller.dto.dashboard.DashboardOverviewResponse;
import com.philxin.interviewos.entity.AppUser;
import com.philxin.interviewos.entity.FeedbackBand;
import com.philxin.interviewos.entity.Knowledge;
import com.philxin.interviewos.entity.KnowledgeSourceType;
import com.philxin.interviewos.entity.KnowledgeStatus;
import com.philxin.interviewos.entity.KnowledgeTag;
import com.philxin.interviewos.entity.TrainingQuestion;
import com.philxin.interviewos.entity.TrainingSession;
import com.philxin.interviewos.repository.KnowledgeRepository;
import com.philxin.interviewos.repository.TrainingQuestionRepository;
import com.philxin.interviewos.repository.TrainingSessionRepository;
import com.philxin.interviewos.security.AuthenticatedUser;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private KnowledgeRepository knowledgeRepository;

    @Mock
    private TrainingSessionRepository trainingSessionRepository;

    @Mock
    private TrainingQuestionRepository trainingQuestionRepository;

    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardService(
            knowledgeRepository,
            trainingSessionRepository,
            trainingQuestionRepository
        );
    }

    @Test
    void getOverviewBuildsWeakItemsRecentTrainingsAndProgressSummary() {
        Knowledge weakKnowledge = buildKnowledge(1L, "Spring Boot 自动配置", 42, List.of("spring", "backend"));
        Knowledge strongerKnowledge = buildKnowledge(2L, "Redis 缓存", 68, List.of("redis"));
        TrainingSession session = buildSession(UUID.fromString("11111111-1111-1111-1111-111111111111"), weakKnowledge, 58);
        TrainingQuestion question = buildQuestion(session, weakKnowledge, 42, 47);

        when(knowledgeRepository.findByUserIdAndStatusOrderByCreatedAtDesc(1L, KnowledgeStatus.ACTIVE))
            .thenReturn(List.of(strongerKnowledge, weakKnowledge));
        when(trainingSessionRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(session));
        when(trainingQuestionRepository.findBySessionIdIn(List.of(session.getId()))).thenReturn(List.of(question));

        DashboardOverviewResponse response = dashboardService.getOverview(authenticatedUser(1L));

        assertEquals(1, response.getRecentTrainings().size());
        assertEquals("Spring Boot 自动配置", response.getRecentTrainings().get(0).getKnowledgeTitle());
        assertEquals(1, response.getWeakKnowledgeItems().get(0).getKnowledgeId());
        assertEquals(1, response.getProgressSummary().getTrainedCountLast7Days());
        assertEquals(58, response.getProgressSummary().getAverageScoreLast7Days());
        assertEquals(1, response.getProgressSummary().getImprovedKnowledgeCount());
    }

    @Test
    void getOverviewReturnsEmptySummaryWhenNoData() {
        when(knowledgeRepository.findByUserIdAndStatusOrderByCreatedAtDesc(1L, KnowledgeStatus.ACTIVE))
            .thenReturn(List.of());
        when(trainingSessionRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());

        DashboardOverviewResponse response = dashboardService.getOverview(authenticatedUser(1L));

        assertEquals(0, response.getWeakKnowledgeItems().size());
        assertEquals(0, response.getRecentTrainings().size());
        assertEquals(0, response.getProgressSummary().getTrainedCountLast7Days());
        assertEquals(0, response.getProgressSummary().getAverageScoreLast7Days());
        assertEquals(0, response.getProgressSummary().getImprovedKnowledgeCount());
    }

    @Test
    void getOverviewThrows401WhenUserMissing() {
        BusinessException exception = assertThrows(BusinessException.class, () -> dashboardService.getOverview(null));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
    }

    private AuthenticatedUser authenticatedUser(Long id) {
        return AuthenticatedUser.fromEntity(buildUser(id));
    }

    private AppUser buildUser(Long id) {
        AppUser user = new AppUser();
        user.setId(id);
        user.setEmail("user@example.com");
        user.setDisplayName("philxin");
        user.setPasswordHash("hash");
        return user;
    }

    private Knowledge buildKnowledge(Long id, String title, int mastery, List<String> tags) {
        Knowledge knowledge = new Knowledge();
        knowledge.setId(id);
        knowledge.setUser(buildUser(1L));
        knowledge.setTitle(title);
        knowledge.setContent("content");
        knowledge.setMastery(mastery);
        knowledge.setSourceType(KnowledgeSourceType.MANUAL);
        knowledge.setStatus(KnowledgeStatus.ACTIVE);
        knowledge.setUpdatedAt(LocalDateTime.now());
        for (String tag : tags) {
            KnowledgeTag knowledgeTag = new KnowledgeTag();
            knowledgeTag.setKnowledge(knowledge);
            knowledgeTag.setTag(tag);
            knowledge.getTags().add(knowledgeTag);
        }
        return knowledge;
    }

    private TrainingSession buildSession(UUID sessionId, Knowledge knowledge, int score) {
        TrainingSession session = new TrainingSession();
        session.setId(sessionId);
        session.setUser(buildUser(1L));
        session.setKnowledge(knowledge);
        session.setSummaryScore(score);
        session.setSummaryBand(FeedbackBand.BASIC);
        session.setCompletedAt(LocalDateTime.now().minusDays(1));
        return session;
    }

    private TrainingQuestion buildQuestion(TrainingSession session, Knowledge knowledge, int before, int after) {
        TrainingQuestion question = new TrainingQuestion();
        question.setId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
        question.setSession(session);
        question.setKnowledge(knowledge);
        question.setMasteryBefore(before);
        question.setMasteryAfter(after);
        return question;
    }
}
