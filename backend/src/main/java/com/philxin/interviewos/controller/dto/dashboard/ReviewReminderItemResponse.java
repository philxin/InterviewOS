package com.philxin.interviewos.controller.dto.dashboard;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 回练提醒项。
 */
public class ReviewReminderItemResponse {
    private Long knowledgeId;
    private String knowledgeTitle;
    private Integer reviewWeight;
    private String reason;
    private String suggestedQuestionType;
    private String suggestedDifficulty;
    private LocalDateTime lastTrainedAt;
    private List<String> tags;

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

    public Integer getReviewWeight() {
        return reviewWeight;
    }

    public void setReviewWeight(Integer reviewWeight) {
        this.reviewWeight = reviewWeight;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getSuggestedQuestionType() {
        return suggestedQuestionType;
    }

    public void setSuggestedQuestionType(String suggestedQuestionType) {
        this.suggestedQuestionType = suggestedQuestionType;
    }

    public String getSuggestedDifficulty() {
        return suggestedDifficulty;
    }

    public void setSuggestedDifficulty(String suggestedDifficulty) {
        this.suggestedDifficulty = suggestedDifficulty;
    }

    public LocalDateTime getLastTrainedAt() {
        return lastTrainedAt;
    }

    public void setLastTrainedAt(LocalDateTime lastTrainedAt) {
        this.lastTrainedAt = lastTrainedAt;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }
}
