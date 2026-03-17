package com.philxin.interviewos.controller.dto.training;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 训练会话摘要。
 */
public class TrainingSessionSummaryResponse {
    private UUID sessionId;
    private Long knowledgeId;
    private String knowledgeTitle;
    private Integer questionCount;
    private Integer answeredCount;
    private Integer sessionScore;
    private FeedbackBandResponse band;
    private String majorIssueSummary;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    public UUID getSessionId() {
        return sessionId;
    }

    public void setSessionId(UUID sessionId) {
        this.sessionId = sessionId;
    }

    public Long getKnowledgeId() {
        return knowledgeId;
    }

    public void setKnowledgeId(Long knowledgeId) {
        this.knowledgeId = knowledgeId;
    }

    public String getKnowledgeTitle() {
        return knowledgeTitle;
    }

    public void setKnowledgeTitle(String knowledgeTitle) {
        this.knowledgeTitle = knowledgeTitle;
    }

    public Integer getQuestionCount() {
        return questionCount;
    }

    public void setQuestionCount(Integer questionCount) {
        this.questionCount = questionCount;
    }

    public Integer getAnsweredCount() {
        return answeredCount;
    }

    public void setAnsweredCount(Integer answeredCount) {
        this.answeredCount = answeredCount;
    }

    public Integer getSessionScore() {
        return sessionScore;
    }

    public void setSessionScore(Integer sessionScore) {
        this.sessionScore = sessionScore;
    }

    public FeedbackBandResponse getBand() {
        return band;
    }

    public void setBand(FeedbackBandResponse band) {
        this.band = band;
    }

    public String getMajorIssueSummary() {
        return majorIssueSummary;
    }

    public void setMajorIssueSummary(String majorIssueSummary) {
        this.majorIssueSummary = majorIssueSummary;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
}
