package com.philxin.interviewos.service;

import com.philxin.interviewos.common.BusinessException;
import com.philxin.interviewos.controller.dto.training.TrainingRecommendationItemResponse;
import com.philxin.interviewos.controller.dto.training.TrainingRecommendationResponse;
import com.philxin.interviewos.entity.Difficulty;
import com.philxin.interviewos.entity.Knowledge;
import com.philxin.interviewos.entity.KnowledgeStatus;
import com.philxin.interviewos.entity.QuestionType;
import com.philxin.interviewos.entity.TrainingSession;
import com.philxin.interviewos.repository.KnowledgeRepository;
import com.philxin.interviewos.repository.TrainingSessionRepository;
import com.philxin.interviewos.security.AuthenticatedUser;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 今日推荐训练包服务。
 */
@Service
public class TrainingRecommendationService {
    private static final int MIN_RECOMMENDATION_COUNT = 3;
    private static final int MAX_RECOMMENDATION_COUNT = 5;

    private final KnowledgeRepository knowledgeRepository;
    private final TrainingSessionRepository trainingSessionRepository;

    public TrainingRecommendationService(
        KnowledgeRepository knowledgeRepository,
        TrainingSessionRepository trainingSessionRepository
    ) {
        this.knowledgeRepository = knowledgeRepository;
        this.trainingSessionRepository = trainingSessionRepository;
    }

    /**
     * 为当前用户生成今日推荐训练包，优先覆盖薄弱且近期未训练的知识点。
     */
    @Transactional(readOnly = true)
    public TrainingRecommendationResponse getTodayRecommendations(AuthenticatedUser authenticatedUser) {
        Long userId = getCurrentUserId(authenticatedUser);
        List<Knowledge> activeKnowledge = knowledgeRepository.findByUserIdAndStatusOrderByCreatedAtDesc(
            userId,
            KnowledgeStatus.ACTIVE
        );
        Map<Long, TrainingSession> latestCompletedSessionByKnowledge = loadLatestCompletedSessionByKnowledge(userId);

        int recommendationSize = resolveRecommendationSize(activeKnowledge.size());
        List<TrainingRecommendationItemResponse> items = activeKnowledge.stream()
            .sorted(buildKnowledgeComparator(latestCompletedSessionByKnowledge))
            .limit(recommendationSize)
            .map(knowledge -> toRecommendationItem(knowledge, latestCompletedSessionByKnowledge.get(knowledge.getId())))
            .toList();

        TrainingRecommendationResponse response = new TrainingRecommendationResponse();
        response.setPackageId(buildPackageId(userId));
        response.setTitle("今日推荐练习");
        response.setItems(items);
        return response;
    }

    private Map<Long, TrainingSession> loadLatestCompletedSessionByKnowledge(Long userId) {
        List<TrainingSession> sessions = trainingSessionRepository.findByUserIdOrderByCreatedAtDesc(userId);
        Map<Long, TrainingSession> latestCompletedSessionByKnowledge = new HashMap<>();
        for (TrainingSession session : sessions) {
            if (session.getCompletedAt() == null || session.getKnowledge() == null || session.getKnowledge().getId() == null) {
                continue;
            }
            Long knowledgeId = session.getKnowledge().getId();
            TrainingSession existing = latestCompletedSessionByKnowledge.get(knowledgeId);
            if (existing == null || session.getCompletedAt().isAfter(existing.getCompletedAt())) {
                latestCompletedSessionByKnowledge.put(knowledgeId, session);
            }
        }
        return latestCompletedSessionByKnowledge;
    }

    private Comparator<Knowledge> buildKnowledgeComparator(Map<Long, TrainingSession> latestCompletedSessionByKnowledge) {
        return Comparator
            .comparingInt((Knowledge knowledge) -> normalizeScore(knowledge.getMastery()))
            .thenComparing(
                knowledge -> resolveCompletedAt(latestCompletedSessionByKnowledge.get(knowledge.getId())),
                Comparator.nullsFirst(Comparator.naturalOrder())
            )
            .thenComparing(Knowledge::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(Knowledge::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private TrainingRecommendationItemResponse toRecommendationItem(Knowledge knowledge, TrainingSession latestSession) {
        QuestionType questionType = resolveQuestionType(knowledge);
        Difficulty difficulty = resolveDifficulty(knowledge, latestSession);

        TrainingRecommendationItemResponse item = new TrainingRecommendationItemResponse();
        item.setKnowledgeId(knowledge.getId());
        item.setQuestionType(questionType.name());
        item.setDifficulty(difficulty.name());
        return item;
    }

    private QuestionType resolveQuestionType(Knowledge knowledge) {
        int mastery = normalizeScore(knowledge.getMastery());
        if (mastery < 45) {
            return QuestionType.FUNDAMENTAL;
        }
        if (mastery < 75) {
            return QuestionType.SCENARIO;
        }
        return QuestionType.PROJECT;
    }

    private Difficulty resolveDifficulty(Knowledge knowledge, TrainingSession latestSession) {
        int mastery = normalizeScore(knowledge.getMastery());
        if (latestSession != null && latestSession.getSummaryScore() != null) {
            int score = normalizeScore(latestSession.getSummaryScore());
            if (score <= 40) {
                return Difficulty.EASY;
            }
            if (score >= 85 && mastery >= 70) {
                return Difficulty.HARD;
            }
        }
        if (mastery < 35) {
            return Difficulty.EASY;
        }
        if (mastery < 75) {
            return Difficulty.MEDIUM;
        }
        return Difficulty.HARD;
    }

    private int resolveRecommendationSize(int totalKnowledgeCount) {
        if (totalKnowledgeCount <= 0) {
            return 0;
        }
        if (totalKnowledgeCount < MIN_RECOMMENDATION_COUNT) {
            return totalKnowledgeCount;
        }
        return Math.min(totalKnowledgeCount, MAX_RECOMMENDATION_COUNT);
    }

    private String buildPackageId(Long userId) {
        return "pkg_today_%s_u%s".formatted(LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE), userId);
    }

    private LocalDateTime resolveCompletedAt(TrainingSession session) {
        return session == null ? null : session.getCompletedAt();
    }

    private Long getCurrentUserId(AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null || authenticatedUser.getId() == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return authenticatedUser.getId();
    }

    private int normalizeScore(Integer score) {
        return score == null ? 0 : Math.max(0, Math.min(100, score));
    }
}
