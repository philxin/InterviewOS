package com.philxin.interviewos.controller.dto.training;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class StartTrainingRequest {

    @NotNull(message = "knowledgeId is required")
    @Min(value = 1, message = "knowledgeId must be greater than or equal to 1")
    private Long knowledgeId;

    public Long getKnowledgeId() {
        return knowledgeId;
    }

    public void setKnowledgeId(Long knowledgeId) {
        this.knowledgeId = knowledgeId;
    }
}
