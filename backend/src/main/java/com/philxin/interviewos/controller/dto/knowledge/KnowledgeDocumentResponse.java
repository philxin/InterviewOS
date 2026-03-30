package com.philxin.interviewos.controller.dto.knowledge;

import com.philxin.interviewos.entity.KnowledgeDocument;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 文档级知识库响应。
 */
public class KnowledgeDocumentResponse {
    private UUID documentId;
    private UUID importId;
    private String title;
    private String originalFileName;
    private String contentType;
    private String status;
    private int totalChunks;
    private int activeChunks;
    private String embeddingModel;
    private Integer embeddingDim;
    private LocalDateTime indexedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static KnowledgeDocumentResponse fromEntity(KnowledgeDocument document) {
        KnowledgeDocumentResponse response = new KnowledgeDocumentResponse();
        response.setDocumentId(document.getId());
        response.setImportId(document.getFileImport() == null ? null : document.getFileImport().getId());
        response.setTitle(document.getTitle());
        response.setOriginalFileName(document.getOriginalFileName());
        response.setContentType(document.getContentType());
        response.setStatus(document.getStatus().name());
        response.setTotalChunks(document.getTotalChunks() == null ? 0 : document.getTotalChunks());
        response.setActiveChunks(document.getActiveChunks() == null ? 0 : document.getActiveChunks());
        response.setEmbeddingModel(document.getEmbeddingModel());
        response.setEmbeddingDim(document.getEmbeddingDim());
        response.setIndexedAt(document.getIndexedAt());
        response.setCreatedAt(document.getCreatedAt());
        response.setUpdatedAt(document.getUpdatedAt());
        return response;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public void setDocumentId(UUID documentId) {
        this.documentId = documentId;
    }

    public UUID getImportId() {
        return importId;
    }

    public void setImportId(UUID importId) {
        this.importId = importId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getTotalChunks() {
        return totalChunks;
    }

    public void setTotalChunks(int totalChunks) {
        this.totalChunks = totalChunks;
    }

    public int getActiveChunks() {
        return activeChunks;
    }

    public void setActiveChunks(int activeChunks) {
        this.activeChunks = activeChunks;
    }

    public String getEmbeddingModel() {
        return embeddingModel;
    }

    public void setEmbeddingModel(String embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public Integer getEmbeddingDim() {
        return embeddingDim;
    }

    public void setEmbeddingDim(Integer embeddingDim) {
        this.embeddingDim = embeddingDim;
    }

    public LocalDateTime getIndexedAt() {
        return indexedAt;
    }

    public void setIndexedAt(LocalDateTime indexedAt) {
        this.indexedAt = indexedAt;
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
