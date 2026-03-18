package com.philxin.interviewos.controller.dto.training;

/**
 * 今日推荐训练项。
 */
public class TrainingRecommendationItemResponse {
    private Long knowledgeId;
    private String questionType;
    private String difficulty;

    public Long getKnowledgeId() {
        return knowledgeId;
    }

    public void setKnowledgeId(Long knowledgeId) {
        this.knowledgeId = knowledgeId;
    }

    public String getQuestionType() {
        return questionType;
    }

    public void setQuestionType(String questionType) {
        this.questionType = questionType;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }
}
