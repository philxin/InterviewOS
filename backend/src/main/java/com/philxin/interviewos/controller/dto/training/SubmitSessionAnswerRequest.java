package com.philxin.interviewos.controller.dto.training;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * 提交训练回答请求。
 */
public class SubmitSessionAnswerRequest {
    @NotNull(message = "questionId must not be null")
    private UUID questionId;

    @NotBlank(message = "answer must not be blank")
    private String answer;

    public UUID getQuestionId() {
        return questionId;
    }

    public void setQuestionId(UUID questionId) {
        this.questionId = questionId;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }
}
