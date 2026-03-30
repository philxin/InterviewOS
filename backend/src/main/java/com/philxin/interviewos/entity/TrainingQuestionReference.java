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
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * 训练问题引用快照。
 */
@Entity
@Table(
    name = "training_question_reference",
    indexes = {
        @Index(name = "idx_tqr_question_usage_rank", columnList = "question_id,usage_type,rank_no"),
        @Index(name = "idx_tqr_chunk_id", columnList = "chunk_id")
    }
)
public class TrainingQuestionReference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false, foreignKey = @ForeignKey(name = "fk_tqr_question_id"))
    private TrainingQuestion question;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chunk_id", foreignKey = @ForeignKey(name = "fk_tqr_chunk_id"))
    private KnowledgeChunk chunk;

    @Enumerated(EnumType.STRING)
    @Column(name = "usage_type", nullable = false, length = 20)
    private TrainingQuestionReferenceUsageType usageType;

    @Column(name = "rank_no", nullable = false)
    private Integer rankNo;

    @Column(name = "similarity_score")
    private Double similarityScore;

    @Column(name = "document_title_snapshot", nullable = false, length = 200)
    private String documentTitleSnapshot;

    @Column(name = "excerpt_snapshot", nullable = false, columnDefinition = "TEXT")
    private String excerptSnapshot;

    @Column(name = "locator_snapshot", columnDefinition = "TEXT")
    private String locatorSnapshot;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
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

    public TrainingQuestion getQuestion() {
        return question;
    }

    public void setQuestion(TrainingQuestion question) {
        this.question = question;
    }

    public KnowledgeChunk getChunk() {
        return chunk;
    }

    public void setChunk(KnowledgeChunk chunk) {
        this.chunk = chunk;
    }

    public TrainingQuestionReferenceUsageType getUsageType() {
        return usageType;
    }

    public void setUsageType(TrainingQuestionReferenceUsageType usageType) {
        this.usageType = usageType;
    }

    public Integer getRankNo() {
        return rankNo;
    }

    public void setRankNo(Integer rankNo) {
        this.rankNo = rankNo;
    }

    public Double getSimilarityScore() {
        return similarityScore;
    }

    public void setSimilarityScore(Double similarityScore) {
        this.similarityScore = similarityScore;
    }

    public String getDocumentTitleSnapshot() {
        return documentTitleSnapshot;
    }

    public void setDocumentTitleSnapshot(String documentTitleSnapshot) {
        this.documentTitleSnapshot = documentTitleSnapshot;
    }

    public String getExcerptSnapshot() {
        return excerptSnapshot;
    }

    public void setExcerptSnapshot(String excerptSnapshot) {
        this.excerptSnapshot = excerptSnapshot;
    }

    public String getLocatorSnapshot() {
        return locatorSnapshot;
    }

    public void setLocatorSnapshot(String locatorSnapshot) {
        this.locatorSnapshot = locatorSnapshot;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
