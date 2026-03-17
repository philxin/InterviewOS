package com.philxin.interviewos.controller.dto.training;

import com.philxin.interviewos.entity.Difficulty;
import com.philxin.interviewos.entity.QuestionType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 启动 V2 训练会话请求。
 */
public class StartTrainingSessionRequest {
    @NotNull(message = "knowledgeId must not be null")
    @Min(value = 1, message = "knowledgeId must be greater than or equal to 1")
    private Long knowledgeId;

    private QuestionType questionType;

    private Difficulty difficulty;

    private Boolean hintEnabled;

    public Long getKnowledgeId() {
        return knowledgeId;
    }

    public void setKnowledgeId(Long knowledgeId) {
        this.knowledgeId = knowledgeId;
    }

    public QuestionType getQuestionType() {
        return questionType;
    }

    public void setQuestionType(QuestionType questionType) {
        this.questionType = questionType;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    public Boolean getHintEnabled() {
        return hintEnabled;
    }

    public void setHintEnabled(Boolean hintEnabled) {
        this.hintEnabled = hintEnabled;
    }
}
