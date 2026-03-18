package com.philxin.interviewos.service;

import com.philxin.interviewos.common.BusinessException;
import com.philxin.interviewos.controller.dto.dashboard.DashboardOverviewResponse;
import com.philxin.interviewos.controller.dto.dashboard.ProgressSummaryResponse;
import com.philxin.interviewos.controller.dto.dashboard.RecentTrainingItemResponse;
import com.philxin.interviewos.controller.dto.dashboard.ReviewReminderItemResponse;
import com.philxin.interviewos.controller.dto.dashboard.ReviewReminderResponse;
import com.philxin.interviewos.controller.dto.dashboard.WeakKnowledgeItemResponse;
import com.philxin.interviewos.controller.dto.training.FeedbackBandResponse;
import com.philxin.interviewos.entity.Difficulty;
import com.philxin.interviewos.entity.Knowledge;
import com.philxin.interviewos.entity.KnowledgeStatus;
import com.philxin.interviewos.entity.QuestionType;
import com.philxin.interviewos.entity.TrainingQuestion;
import com.philxin.interviewos.entity.TrainingSession;
import com.philxin.interviewos.repository.KnowledgeRepository;
import com.philxin.interviewos.repository.TrainingQuestionRepository;
import com.philxin.interviewos.repository.TrainingSessionRepository;
import com.philxin.interviewos.security.AuthenticatedUser;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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
    private static final int REVIEW_REMINDER_LIMIT = 3;
    private static final int REVIEW_RECENT_SESSION_WINDOW = 3;

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

    /**
     * 生成当前用户的回练提醒列表，按权重降序输出。
     */
    @Transactional(readOnly = true)
    public ReviewReminderResponse getReviewReminders(AuthenticatedUser authenticatedUser) {
        Long userId = getCurrentUserId(authenticatedUser);
        List<Knowledge> activeKnowledge = knowledgeRepository.findByUserIdAndStatusOrderByCreatedAtDesc(
            userId,
            KnowledgeStatus.ACTIVE
        );
        List<TrainingSession> completedSessions = trainingSessionRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
            .filter(session -> session.getCompletedAt() != null)
            .toList();

        Map<Long, List<TrainingSession>> completedSessionsByKnowledgeId = completedSessions.stream()
            .filter(session -> session.getKnowledge() != null && session.getKnowledge().getId() != null)
            .collect(Collectors.groupingBy(session -> session.getKnowledge().getId()));

        List<ReviewReminderItemResponse> items = activeKnowledge.stream()
            .map(knowledge -> toReviewReminderItem(
                knowledge,
                completedSessionsByKnowledgeId.getOrDefault(knowledge.getId(), List.of())
            ))
            .sorted(
                Comparator.comparingInt(ReviewReminderItemResponse::getReviewWeight)
                    .reversed()
                    .thenComparing(ReviewReminderItemResponse::getLastTrainedAt, Comparator.nullsFirst(Comparator.naturalOrder()))
                    .thenComparing(ReviewReminderItemResponse::getKnowledgeId)
            )
            .limit(REVIEW_REMINDER_LIMIT)
            .toList();

        ReviewReminderResponse response = new ReviewReminderResponse();
        response.setItems(items);
        response.setGeneratedAt(LocalDateTime.now());
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

    private ReviewReminderItemResponse toReviewReminderItem(Knowledge knowledge, List<TrainingSession> sessions) {
        TrainingSession latestSession = sessions.stream()
            .max(Comparator.comparing(TrainingSession::getCompletedAt))
            .orElse(null);
        int mastery = normalizeScore(knowledge.getMastery());
        int recentAverageScore = resolveRecentAverageScore(sessions);
        int daysSinceLastTraining = resolveDaysSinceLastTraining(latestSession);
        int reviewWeight = resolveReviewWeight(mastery, recentAverageScore, daysSinceLastTraining, latestSession == null);

        Suggestion suggestion = resolveSuggestion(mastery, recentAverageScore);

        ReviewReminderItemResponse item = new ReviewReminderItemResponse();
        item.setKnowledgeId(knowledge.getId());
        item.setKnowledgeTitle(knowledge.getTitle());
        item.setReviewWeight(reviewWeight);
        item.setReason(resolveReminderReason(mastery, recentAverageScore, daysSinceLastTraining, latestSession == null));
        item.setSuggestedQuestionType(suggestion.questionType().name());
        item.setSuggestedDifficulty(suggestion.difficulty().name());
        item.setLastTrainedAt(latestSession == null ? null : latestSession.getCompletedAt());
        item.setTags(knowledge.getTags().stream().map(tag -> tag.getTag()).toList());
        return item;
    }

    private int resolveReviewWeight(
        int mastery,
        int recentAverageScore,
        int daysSinceLastTraining,
        boolean neverTrained
    ) {
        int weight = 0;
        weight += Math.max(0, 65 - mastery);
        if (recentAverageScore >= 0) {
            weight += Math.max(0, 70 - recentAverageScore);
        } else {
            weight += 20;
        }
        if (neverTrained) {
            weight += 18;
        } else {
            weight += Math.min(20, daysSinceLastTraining);
        }
        return Math.max(0, Math.min(100, weight));
    }

    private String resolveReminderReason(
        int mastery,
        int recentAverageScore,
        int daysSinceLastTraining,
        boolean neverTrained
    ) {
        if (neverTrained) {
            return "尚未开始该知识点训练，建议先完成一次基础回练。";
        }
        if (mastery < 50 && recentAverageScore >= 0 && recentAverageScore < 60) {
            return "掌握度和最近得分都偏低，建议优先回练巩固关键点。";
        }
        if (daysSinceLastTraining >= 7) {
            return "距离上次训练已超过 %s 天，建议尽快复训避免遗忘。".formatted(daysSinceLastTraining);
        }
        if (recentAverageScore >= 0 && recentAverageScore < 65) {
            return "最近表现有波动，建议补一次场景题强化表达稳定性。";
        }
        return "建议继续保持训练节奏，巩固当前知识点表现。";
    }

    private Suggestion resolveSuggestion(int mastery, int recentAverageScore) {
        if (recentAverageScore >= 0 && recentAverageScore < 50) {
            return new Suggestion(QuestionType.FUNDAMENTAL, Difficulty.EASY);
        }
        if (mastery < 40) {
            return new Suggestion(QuestionType.FUNDAMENTAL, Difficulty.EASY);
        }
        if (mastery < 70) {
            return new Suggestion(QuestionType.SCENARIO, Difficulty.MEDIUM);
        }
        if (recentAverageScore >= 80) {
            return new Suggestion(QuestionType.PROJECT, Difficulty.HARD);
        }
        return new Suggestion(QuestionType.PROJECT, Difficulty.MEDIUM);
    }

    private int resolveRecentAverageScore(List<TrainingSession> sessions) {
        return (int) Math.round(
            sessions.stream()
                .sorted(Comparator.comparing(TrainingSession::getCompletedAt).reversed())
                .limit(REVIEW_RECENT_SESSION_WINDOW)
                .map(TrainingSession::getSummaryScore)
                .filter(score -> score != null)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(-1)
        );
    }

    private int resolveDaysSinceLastTraining(TrainingSession latestSession) {
        if (latestSession == null || latestSession.getCompletedAt() == null) {
            return Integer.MAX_VALUE;
        }
        long days = Duration.between(latestSession.getCompletedAt(), LocalDateTime.now()).toDays();
        return (int) Math.max(0, days);
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

    private record Suggestion(QuestionType questionType, Difficulty difficulty) {
    }
}
