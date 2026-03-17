package com.philxin.interviewos.service;

import com.philxin.interviewos.common.BusinessException;
import com.philxin.interviewos.controller.dto.dashboard.DashboardOverviewResponse;
import com.philxin.interviewos.controller.dto.dashboard.ProgressSummaryResponse;
import com.philxin.interviewos.controller.dto.dashboard.RecentTrainingItemResponse;
import com.philxin.interviewos.controller.dto.dashboard.WeakKnowledgeItemResponse;
import com.philxin.interviewos.controller.dto.training.FeedbackBandResponse;
import com.philxin.interviewos.entity.Knowledge;
import com.philxin.interviewos.entity.KnowledgeStatus;
import com.philxin.interviewos.entity.TrainingQuestion;
import com.philxin.interviewos.entity.TrainingSession;
import com.philxin.interviewos.repository.KnowledgeRepository;
import com.philxin.interviewos.repository.TrainingQuestionRepository;
import com.philxin.interviewos.repository.TrainingSessionRepository;
import com.philxin.interviewos.security.AuthenticatedUser;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 首页概览聚合服务。
 */
@Service
public class DashboardService {
    private static final int WEAK_ITEM_LIMIT = 3;
    private static final int RECENT_TRAINING_LIMIT = 3;

    private final KnowledgeRepository knowledgeRepository;
    private final TrainingSessionRepository trainingSessionRepository;
    private final TrainingQuestionRepository trainingQuestionRepository;

    public DashboardService(
        KnowledgeRepository knowledgeRepository,
        TrainingSessionRepository trainingSessionRepository,
        TrainingQuestionRepository trainingQuestionRepository
    ) {
        this.knowledgeRepository = knowledgeRepository;
        this.trainingSessionRepository = trainingSessionRepository;
        this.trainingQuestionRepository = trainingQuestionRepository;
    }

    /**
     * 聚合当前用户首页所需的薄弱项、最近训练与近 7 天进步摘要。
     */
    @Transactional(readOnly = true)
    public DashboardOverviewResponse getOverview(AuthenticatedUser authenticatedUser) {
        Long userId = getCurrentUserId(authenticatedUser);
        List<Knowledge> activeKnowledge = knowledgeRepository.findByUserIdAndStatusOrderByCreatedAtDesc(
            userId,
            KnowledgeStatus.ACTIVE
        );
        List<TrainingSession> sessions = trainingSessionRepository.findByUserIdOrderByCreatedAtDesc(userId);
        List<TrainingSession> completedSessions = sessions.stream()
            .filter(session -> session.getCompletedAt() != null)
            .sorted(Comparator.comparing(TrainingSession::getCompletedAt).reversed())
            .toList();

        DashboardOverviewResponse response = new DashboardOverviewResponse();
        response.setWeakKnowledgeItems(buildWeakKnowledgeItems(activeKnowledge));
        response.setRecentTrainings(buildRecentTrainings(completedSessions));
        response.setProgressSummary(buildProgressSummary(completedSessions));
        return response;
    }

    private List<WeakKnowledgeItemResponse> buildWeakKnowledgeItems(List<Knowledge> knowledgeList) {
        return knowledgeList.stream()
            .sorted(
                Comparator.comparing((Knowledge knowledge) -> normalizeScore(knowledge.getMastery()))
                    .thenComparing(Knowledge::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
            )
            .limit(WEAK_ITEM_LIMIT)
            .map(this::toWeakKnowledgeItem)
            .toList();
    }

    private List<RecentTrainingItemResponse> buildRecentTrainings(List<TrainingSession> completedSessions) {
        return completedSessions.stream()
            .limit(RECENT_TRAINING_LIMIT)
            .map(this::toRecentTrainingItem)
            .toList();
    }

    private ProgressSummaryResponse buildProgressSummary(List<TrainingSession> completedSessions) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(7);
        List<TrainingSession> last7DaysSessions = completedSessions.stream()
            .filter(session -> session.getCompletedAt() != null && !session.getCompletedAt().isBefore(cutoff))
            .toList();

        ProgressSummaryResponse response = new ProgressSummaryResponse();
        response.setTrainedCountLast7Days(last7DaysSessions.size());
        response.setAverageScoreLast7Days(resolveAverageScore(last7DaysSessions));
        response.setImprovedKnowledgeCount(resolveImprovedKnowledgeCount(last7DaysSessions));
        return response;
    }

    private int resolveAverageScore(List<TrainingSession> sessions) {
        return (int) Math.round(
            sessions.stream()
                .map(TrainingSession::getSummaryScore)
                .filter(score -> score != null)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0)
        );
    }

    private int resolveImprovedKnowledgeCount(List<TrainingSession> sessions) {
        if (sessions.isEmpty()) {
            return 0;
        }
        List<UUID> sessionIds = sessions.stream().map(TrainingSession::getId).toList();
        List<TrainingQuestion> questions = trainingQuestionRepository.findBySessionIdIn(sessionIds);
        Set<Long> improvedKnowledgeIds = questions.stream()
            .filter(question -> question.getMasteryBefore() != null && question.getMasteryAfter() != null)
            .filter(question -> question.getMasteryAfter() > question.getMasteryBefore())
            .map(question -> question.getKnowledge().getId())
            .collect(Collectors.toSet());
        return improvedKnowledgeIds.size();
    }

    private WeakKnowledgeItemResponse toWeakKnowledgeItem(Knowledge knowledge) {
        WeakKnowledgeItemResponse response = new WeakKnowledgeItemResponse();
        response.setKnowledgeId(knowledge.getId());
        response.setTitle(knowledge.getTitle());
        response.setMastery(normalizeScore(knowledge.getMastery()));
        response.setTags(knowledge.getTags().stream().map(tag -> tag.getTag()).toList());
        return response;
    }

    private RecentTrainingItemResponse toRecentTrainingItem(TrainingSession session) {
        RecentTrainingItemResponse response = new RecentTrainingItemResponse();
        response.setSessionId(session.getId());
        response.setKnowledgeId(session.getKnowledge().getId());
        response.setKnowledgeTitle(session.getKnowledge().getTitle());
        response.setSessionScore(session.getSummaryScore());
        response.setBand(FeedbackBandResponse.fromBand(session.getSummaryBand()));
        response.setCompletedAt(session.getCompletedAt());
        return response;
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
