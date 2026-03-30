package com.philxin.interviewos.controller.dto.knowledge;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 候选知识点响应。
 */
public class KnowledgeConceptResponse {
    private Long conceptId;
    private UUID documentId;
    private String name;
    private String summary;
    private List<String> aliases;
    private List<Long> supportingChunkIds;
    private Double confidence;
    private String status;
    private Long acceptedKnowledgeId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getConceptId() {
        return conceptId;
    }

    public void setConceptId(Long conceptId) {
        this.conceptId = conceptId;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public void setDocumentId(UUID documentId) {
        this.documentId = documentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<String> getAliases() {
        return aliases;
    }

    public void setAliases(List<String> aliases) {
        this.aliases = aliases;
    }

    public List<Long> getSupportingChunkIds() {
        return supportingChunkIds;
    }

    public void setSupportingChunkIds(List<Long> supportingChunkIds) {
        this.supportingChunkIds = supportingChunkIds;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getAcceptedKnowledgeId() {
        return acceptedKnowledgeId;
    }

    public void setAcceptedKnowledgeId(Long acceptedKnowledgeId) {
        this.acceptedKnowledgeId = acceptedKnowledgeId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
