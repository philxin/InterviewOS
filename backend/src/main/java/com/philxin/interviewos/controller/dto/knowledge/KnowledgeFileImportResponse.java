package com.philxin.interviewos.controller.dto.knowledge;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 文件导入任务状态响应。
 */
public class KnowledgeFileImportResponse {
    private UUID importId;
    private UUID documentId;
    private String fileName;
    private String contentType;
    private long fileSize;
    private String status;
    private List<String> defaultTags;
    private int createdCount;
    private int totalChunks;
    private int embeddedChunks;
    private int failedChunks;
    private String embeddingModel;
    private Integer embeddingDim;
    private String failureReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;

    public UUID getImportId() {
        return importId;
    }

    public void setImportId(UUID importId) {
        this.importId = importId;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public void setDocumentId(UUID documentId) {
        this.documentId = documentId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<String> getDefaultTags() {
        return defaultTags;
    }

    public void setDefaultTags(List<String> defaultTags) {
        this.defaultTags = defaultTags;
    }

    public int getCreatedCount() {
        return createdCount;
    }

    public void setCreatedCount(int createdCount) {
        this.createdCount = createdCount;
    }

    public int getTotalChunks() {
        return totalChunks;
    }

    public void setTotalChunks(int totalChunks) {
        this.totalChunks = totalChunks;
    }

    public int getEmbeddedChunks() {
        return embeddedChunks;
    }

    public void setEmbeddedChunks(int embeddedChunks) {
        this.embeddedChunks = embeddedChunks;
    }

    public int getFailedChunks() {
        return failedChunks;
    }

    public void setFailedChunks(int failedChunks) {
        this.failedChunks = failedChunks;
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

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
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

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
}
