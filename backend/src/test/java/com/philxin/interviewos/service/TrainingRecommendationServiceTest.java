package com.philxin.interviewos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.philxin.interviewos.common.BusinessException;
import com.philxin.interviewos.controller.dto.training.TrainingRecommendationResponse;
import com.philxin.interviewos.entity.AppUser;
import com.philxin.interviewos.entity.Knowledge;
import com.philxin.interviewos.entity.KnowledgeSourceType;
import com.philxin.interviewos.entity.KnowledgeStatus;
import com.philxin.interviewos.entity.QuestionType;
import com.philxin.interviewos.entity.TrainingSession;
import com.philxin.interviewos.repository.KnowledgeRepository;
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
class TrainingRecommendationServiceTest {

    @Mock
    private KnowledgeRepository knowledgeRepository;

    @Mock
    private TrainingSessionRepository trainingSessionRepository;

    private TrainingRecommendationService trainingRecommendationService;

    @BeforeEach
    void setUp() {
        trainingRecommendationService = new TrainingRecommendationService(
            knowledgeRepository,
            trainingSessionRepository
        );
    }

    @Test
    void getTodayRecommendationsReturnsFiveItemsWhenKnowledgeIsEnough() {
        List<Knowledge> knowledgeList = List.of(
            buildKnowledge(1L, 20),
            buildKnowledge(2L, 60),
            buildKnowledge(3L, 35),
            buildKnowledge(4L, 82),
            buildKnowledge(5L, 48),
            buildKnowledge(6L, 10)
        );
        when(knowledgeRepository.findByUserIdAndStatusOrderByCreatedAtDesc(1L, KnowledgeStatus.ACTIVE))
            .thenReturn(knowledgeList);
        when(trainingSessionRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());

        TrainingRecommendationResponse response = trainingRecommendationService.getTodayRecommendations(authenticatedUser());

        assertEquals(5, response.getItems().size());
        assertTrue(response.getPackageId().startsWith("pkg_today_"));
        assertEquals("今日推荐练习", response.getTitle());
    }

    @Test
    void getTodayRecommendationsResolvesQuestionTypeAndDifficulty() {
        Knowledge weakKnowledge = buildKnowledge(1L, 20);
        Knowledge middleKnowledge = buildKnowledge(2L, 50);
        Knowledge strongKnowledge = buildKnowledge(3L, 82);
        when(knowledgeRepository.findByUserIdAndStatusOrderByCreatedAtDesc(1L, KnowledgeStatus.ACTIVE))
            .thenReturn(List.of(strongKnowledge, middleKnowledge, weakKnowledge));
        when(trainingSessionRepository.findByUserIdOrderByCreatedAtDesc(1L))
            .thenReturn(List.of(buildCompletedSession(strongKnowledge, 90)));

        TrainingRecommendationResponse response = trainingRecommendationService.getTodayRecommendations(authenticatedUser());

        assertEquals(3, response.getItems().size());
        assertEquals(1L, response.getItems().get(0).getKnowledgeId());
        assertEquals("FUNDAMENTAL", response.getItems().get(0).getQuestionType());
        assertEquals("EASY", response.getItems().get(0).getDifficulty());
        assertEquals(2L, response.getItems().get(1).getKnowledgeId());
        assertEquals("SCENARIO", response.getItems().get(1).getQuestionType());
        assertEquals("MEDIUM", response.getItems().get(1).getDifficulty());
        assertEquals(3L, response.getItems().get(2).getKnowledgeId());
        assertEquals("PROJECT", response.getItems().get(2).getQuestionType());
        assertEquals("HARD", response.getItems().get(2).getDifficulty());
    }

    @Test
    void getTodayRecommendationsThrows401WhenUserMissing() {
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> trainingRecommendationService.getTodayRecommendations(null)
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        assertEquals("Unauthorized", exception.getMessage());
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
        knowledge.setTitle("Knowledge-" + id);
        knowledge.setContent("content");
        knowledge.setMastery(mastery);
        knowledge.setSourceType(KnowledgeSourceType.MANUAL);
        knowledge.setStatus(KnowledgeStatus.ACTIVE);
        knowledge.setCreatedAt(LocalDateTime.of(2026, 3, 18, 0, 0));
        knowledge.setUpdatedAt(LocalDateTime.of(2026, 3, 18, 0, 0));
        return knowledge;
    }

    private TrainingSession buildCompletedSession(Knowledge knowledge, Integer score) {
        TrainingSession session = new TrainingSession();
        session.setId(UUID.randomUUID());
        session.setUser(buildUser());
        session.setKnowledge(knowledge);
        session.setQuestionType(QuestionType.PROJECT);
        session.setDifficulty(com.philxin.interviewos.entity.Difficulty.HARD);
        session.setSummaryScore(score);
        session.setCompletedAt(LocalDateTime.of(2026, 3, 17, 9, 0));
        return session;
    }
}
