package com.philxin.interviewos.controller.dto.dashboard;

import com.philxin.interviewos.controller.dto.training.FeedbackBandResponse;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 最近训练摘要项。
 */
public class RecentTrainingItemResponse {
    private UUID sessionId;
    private Long knowledgeId;
    private String knowledgeTitle;
    private Integer sessionScore;
    private FeedbackBandResponse band;
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

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
}
