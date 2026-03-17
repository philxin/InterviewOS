package com.philxin.interviewos.controller.dto.dashboard;

import java.util.List;

/**
 * 首页概览响应。
 */
public class DashboardOverviewResponse {
    private List<WeakKnowledgeItemResponse> weakKnowledgeItems;
    private List<RecentTrainingItemResponse> recentTrainings;
    private ProgressSummaryResponse progressSummary;

    public List<WeakKnowledgeItemResponse> getWeakKnowledgeItems() {
        return weakKnowledgeItems;
    }

    public void setWeakKnowledgeItems(List<WeakKnowledgeItemResponse> weakKnowledgeItems) {
        this.weakKnowledgeItems = weakKnowledgeItems;
    }

    public List<RecentTrainingItemResponse> getRecentTrainings() {
        return recentTrainings;
    }

    public void setRecentTrainings(List<RecentTrainingItemResponse> recentTrainings) {
        this.recentTrainings = recentTrainings;
    }

    public ProgressSummaryResponse getProgressSummary() {
        return progressSummary;
    }

    public void setProgressSummary(ProgressSummaryResponse progressSummary) {
        this.progressSummary = progressSummary;
    }
}
