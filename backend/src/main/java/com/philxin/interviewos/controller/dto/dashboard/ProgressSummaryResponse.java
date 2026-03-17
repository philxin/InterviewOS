package com.philxin.interviewos.controller.dto.dashboard;

/**
 * 最近进步摘要。
 */
public class ProgressSummaryResponse {
    private int trainedCountLast7Days;
    private int averageScoreLast7Days;
    private int improvedKnowledgeCount;

    public int getTrainedCountLast7Days() {
        return trainedCountLast7Days;
    }

    public void setTrainedCountLast7Days(int trainedCountLast7Days) {
        this.trainedCountLast7Days = trainedCountLast7Days;
    }

    public int getAverageScoreLast7Days() {
        return averageScoreLast7Days;
    }

    public void setAverageScoreLast7Days(int averageScoreLast7Days) {
        this.averageScoreLast7Days = averageScoreLast7Days;
    }

    public int getImprovedKnowledgeCount() {
        return improvedKnowledgeCount;
    }

    public void setImprovedKnowledgeCount(int improvedKnowledgeCount) {
        this.improvedKnowledgeCount = improvedKnowledgeCount;
    }
}
