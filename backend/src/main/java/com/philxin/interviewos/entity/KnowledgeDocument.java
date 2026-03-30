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
 * V3 文件级知识文档。
 */
@Entity
@Table(
    name = "knowledge_document",
    indexes = {
        @Index(name = "uk_knowledge_document_import_id", columnList = "import_id", unique = true),
        @Index(name = "idx_knowledge_document_user_status_updated", columnList = "user_id,status,updated_at"),
        @Index(name = "idx_knowledge_document_user_created", columnList = "user_id,created_at"),
        @Index(name = "idx_knowledge_document_content_hash", columnList = "user_id,content_hash")
    }
)
public class KnowledgeDocument {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_knowledge_document_user_id"))
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "import_id", nullable = false, foreignKey = @ForeignKey(name = "fk_knowledge_document_import_id"))
    private KnowledgeFileImport fileImport;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "original_file_name", nullable = false, length = 255)
    private String originalFileName;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;

    @Column(name = "parser_version", length = 50)
    private String parserVersion;

    @Column(name = "chunk_strategy", nullable = false, length = 50)
    private String chunkStrategy;

    @Column(name = "embedding_model", length = 100)
    private String embeddingModel;

    @Column(name = "embedding_dim")
    private Integer embeddingDim;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private KnowledgeDocumentStatus status;

    @Column(name = "total_chunks", nullable = false)
    private Integer totalChunks;

    @Column(name = "active_chunks", nullable = false)
    private Integer activeChunks;

    @Column(name = "source_meta", columnDefinition = "TEXT")
    private String sourceMeta;

    @Column(name = "indexed_at")
    private LocalDateTime indexedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (status == null) {
            status = KnowledgeDocumentStatus.PROCESSING;
        }
        if (totalChunks == null) {
            totalChunks = 0;
        }
        if (activeChunks == null) {
            activeChunks = 0;
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

    public KnowledgeFileImport getFileImport() {
        return fileImport;
    }

    public void setFileImport(KnowledgeFileImport fileImport) {
        this.fileImport = fileImport;
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

    public String getContentHash() {
        return contentHash;
    }

    public void setContentHash(String contentHash) {
        this.contentHash = contentHash;
    }

    public String getParserVersion() {
        return parserVersion;
    }

    public void setParserVersion(String parserVersion) {
        this.parserVersion = parserVersion;
    }

    public String getChunkStrategy() {
        return chunkStrategy;
    }

    public void setChunkStrategy(String chunkStrategy) {
        this.chunkStrategy = chunkStrategy;
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

    public KnowledgeDocumentStatus getStatus() {
        return status;
    }

    public void setStatus(KnowledgeDocumentStatus status) {
        this.status = status;
    }

    public Integer getTotalChunks() {
        return totalChunks;
    }

    public void setTotalChunks(Integer totalChunks) {
        this.totalChunks = totalChunks;
    }

    public Integer getActiveChunks() {
        return activeChunks;
    }

    public void setActiveChunks(Integer activeChunks) {
        this.activeChunks = activeChunks;
    }

    public String getSourceMeta() {
        return sourceMeta;
    }

    public void setSourceMeta(String sourceMeta) {
        this.sourceMeta = sourceMeta;
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

    public LocalDateTime getArchivedAt() {
        return archivedAt;
    }

    public void setArchivedAt(LocalDateTime archivedAt) {
        this.archivedAt = archivedAt;
    }
}
