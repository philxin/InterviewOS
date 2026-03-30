package com.philxin.interviewos.controller.dto.training;

import java.util.List;

/**
 * 训练提示响应。
 */
public class TrainingHintResponse {
    private String hint;
    private String retrievalMode;
    private List<TrainingReferenceResponse> references;

    public String getHint() {
        return hint;
    }

    public void setHint(String hint) {
        this.hint = hint;
    }

    public String getRetrievalMode() {
        return retrievalMode;
    }

    public void setRetrievalMode(String retrievalMode) {
        this.retrievalMode = retrievalMode;
    }

    public List<TrainingReferenceResponse> getReferences() {
        return references;
    }

    public void setReferences(List<TrainingReferenceResponse> references) {
        this.references = references;
    }
}
