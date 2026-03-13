package com.philxin.interviewos.controller.dto.training;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class SubmitTrainingRequest {

    @NotNull(message = "knowledgeId is required")
    @Min(value = 1, message = "knowledgeId must be greater than or equal to 1")
    private Long knowledgeId;

    @NotBlank(message = "question is required")
    private String question;

    @NotBlank(message = "answer is required")
    private String answer;

    public Long getKnowledgeId() {
        return knowledgeId;
    }

    public void setKnowledgeId(Long knowledgeId) {
        this.knowledgeId = knowledgeId;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }
}
