package com.philxin.interviewos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * 自动识别知识点候选实体。
 */
@Entity
@Table(
    name = "knowledge_concept",
    indexes = {
        @Index(name = "idx_knowledge_concept_user_doc_status_updated", columnList = "user_id,document_id,status,updated_at"),
        @Index(name = "idx_knowledge_concept_user_status_confidence_created", columnList = "user_id,status,confidence,created_at"),
        @Index(name = "idx_knowledge_concept_accepted_knowledge", columnList = "accepted_knowledge_id")
    }
)
public class KnowledgeConcept {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_knowledge_concept_user_id"))
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false, foreignKey = @ForeignKey(name = "fk_knowledge_concept_document_id"))
    private KnowledgeDocument document;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String aliases;

    @Column(name = "supporting_chunk_ids", nullable = false, columnDefinition = "TEXT")
    private String supportingChunkIds;

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal confidence;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private KnowledgeConceptStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accepted_knowledge_id", foreignKey = @ForeignKey(name = "fk_knowledge_concept_accepted_knowledge_id"))
    private Knowledge acceptedKnowledge;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (status == null) {
            status = KnowledgeConceptStatus.CANDIDATE;
        }
        if (confidence == null) {
            confidence = BigDecimal.valueOf(0.5d).setScale(4, RoundingMode.HALF_UP);
        } else {
            confidence = confidence.setScale(4, RoundingMode.HALF_UP);
        }
        if (supportingChunkIds == null || supportingChunkIds.isBlank()) {
            supportingChunkIds = "[]";
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
        if (confidence != null) {
            confidence = confidence.setScale(4, RoundingMode.HALF_UP);
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AppUser getUser() {
        return user;
    }

    public void setUser(AppUser user) {
        this.user = user;
    }

    public KnowledgeDocument getDocument() {
        return document;
    }

    public void setDocument(KnowledgeDocument document) {
        this.document = document;
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

    public String getAliases() {
        return aliases;
    }

    public void setAliases(String aliases) {
        this.aliases = aliases;
    }

    public String getSupportingChunkIds() {
        return supportingChunkIds;
    }

    public void setSupportingChunkIds(String supportingChunkIds) {
        this.supportingChunkIds = supportingChunkIds;
    }

    public BigDecimal getConfidence() {
        return confidence;
    }

    public void setConfidence(BigDecimal confidence) {
        this.confidence = confidence;
    }

    public KnowledgeConceptStatus getStatus() {
        return status;
    }

    public void setStatus(KnowledgeConceptStatus status) {
        this.status = status;
    }

    public Knowledge getAcceptedKnowledge() {
        return acceptedKnowledge;
    }

    public void setAcceptedKnowledge(Knowledge acceptedKnowledge) {
        this.acceptedKnowledge = acceptedKnowledge;
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
