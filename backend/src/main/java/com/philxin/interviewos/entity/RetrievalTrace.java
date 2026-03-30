package com.philxin.interviewos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 检索 trace 记录：用于回归与可观测性分析。
 */
@Entity
@Table(
    name = "retrieval_trace",
    indexes = {
        @Index(name = "idx_retrieval_trace_user_created", columnList = "user_id,created_at"),
        @Index(name = "idx_retrieval_trace_user_document_created", columnList = "user_id,document_id,created_at")
    }
)
public class RetrievalTrace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "document_id")
    private UUID documentId;

    @Column(name = "query_fingerprint", nullable = false, length = 64)
    private String queryFingerprint;

    @Column(name = "query_length", nullable = false)
    private Integer queryLength;

    @Column(name = "top_k", nullable = false)
    private Integer topK;

    @Column(name = "hit_count", nullable = false)
    private Integer hitCount;

    @Column(name = "degraded", nullable = false)
    private boolean degraded;

    @Column(name = "chunk_ids", nullable = false, columnDefinition = "TEXT")
    private String chunkIds;

    @Column(name = "score_distribution", nullable = false, columnDefinition = "TEXT")
    private String scoreDistribution;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (chunkIds == null) {
            chunkIds = "[]";
        }
        if (scoreDistribution == null) {
            scoreDistribution = "{}";
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public void setDocumentId(UUID documentId) {
        this.documentId = documentId;
    }

    public String getQueryFingerprint() {
        return queryFingerprint;
    }

    public void setQueryFingerprint(String queryFingerprint) {
        this.queryFingerprint = queryFingerprint;
    }

    public Integer getQueryLength() {
        return queryLength;
    }

    public void setQueryLength(Integer queryLength) {
        this.queryLength = queryLength;
    }

    public Integer getTopK() {
        return topK;
    }

    public void setTopK(Integer topK) {
        this.topK = topK;
    }

    public Integer getHitCount() {
        return hitCount;
    }

    public void setHitCount(Integer hitCount) {
        this.hitCount = hitCount;
    }

    public boolean isDegraded() {
        return degraded;
    }

    public void setDegraded(boolean degraded) {
        this.degraded = degraded;
    }

    public String getChunkIds() {
        return chunkIds;
    }

    public void setChunkIds(String chunkIds) {
        this.chunkIds = chunkIds;
    }

    public String getScoreDistribution() {
        return scoreDistribution;
    }

    public void setScoreDistribution(String scoreDistribution) {
        this.scoreDistribution = scoreDistribution;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
