package com.philxin.interviewos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 文件导入任务实体，对应 knowledge_file_import 表。
 */
@Entity
@Table(
    name = "knowledge_file_import",
    indexes = {
        @Index(name = "idx_file_import_user_created", columnList = "user_id,created_at"),
        @Index(name = "idx_file_import_status_created", columnList = "status,created_at")
    }
)
public class KnowledgeFileImport {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "user_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_knowledge_file_import_user_id")
    )
    private AppUser user;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "document_id")
    private UUID documentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private KnowledgeFileImportStatus status;

    @Column(name = "default_tags", columnDefinition = "TEXT")
    private String defaultTags;

    @Column(name = "created_count", nullable = false)
    private Integer createdCount;

    @Column(name = "content_hash", length = 64)
    private String contentHash;

    @Column(name = "total_chunks", nullable = false)
    private Integer totalChunks;

    @Column(name = "embedded_chunks", nullable = false)
    private Integer embeddedChunks;

    @Column(name = "failed_chunks", nullable = false)
    private Integer failedChunks;

    @Column(name = "parser_version", length = 50)
    private String parserVersion;

    @Column(name = "embedding_model", length = 100)
    private String embeddingModel;

    @Column(name = "embedding_dim")
    private Integer embeddingDim;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (status == null) {
            status = KnowledgeFileImportStatus.PENDING;
        }
        if (createdCount == null) {
            createdCount = 0;
        }
        if (totalChunks == null) {
            totalChunks = 0;
        }
        if (embeddedChunks == null) {
            embeddedChunks = 0;
        }
        if (failedChunks == null) {
            failedChunks = 0;
        }
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public AppUser getUser() {
        return user;
    }

    public void setUser(AppUser user) {
        this.user = user;
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

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public void setDocumentId(UUID documentId) {
        this.documentId = documentId;
    }

    public KnowledgeFileImportStatus getStatus() {
        return status;
    }

    public void setStatus(KnowledgeFileImportStatus status) {
        this.status = status;
    }

    public String getDefaultTags() {
        return defaultTags;
    }

    public void setDefaultTags(String defaultTags) {
        this.defaultTags = defaultTags;
    }

    public Integer getCreatedCount() {
        return createdCount;
    }

    public void setCreatedCount(Integer createdCount) {
        this.createdCount = createdCount;
    }

    public String getContentHash() {
        return contentHash;
    }

    public void setContentHash(String contentHash) {
        this.contentHash = contentHash;
    }

    public Integer getTotalChunks() {
        return totalChunks;
    }

    public void setTotalChunks(Integer totalChunks) {
        this.totalChunks = totalChunks;
    }

    public Integer getEmbeddedChunks() {
        return embeddedChunks;
    }

    public void setEmbeddedChunks(Integer embeddedChunks) {
        this.embeddedChunks = embeddedChunks;
    }

    public Integer getFailedChunks() {
        return failedChunks;
    }

    public void setFailedChunks(Integer failedChunks) {
        this.failedChunks = failedChunks;
    }

    public String getParserVersion() {
        return parserVersion;
    }

    public void setParserVersion(String parserVersion) {
        this.parserVersion = parserVersion;
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
