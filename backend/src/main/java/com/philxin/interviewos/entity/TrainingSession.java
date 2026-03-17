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
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import org.hibernate.annotations.Check;

/**
 * V2 训练会话主表。
 */
@Entity
@Table(
    name = "training_session",
    indexes = {
        @Index(name = "idx_training_session_user_created", columnList = "user_id,created_at"),
        @Index(name = "idx_training_session_user_status", columnList = "user_id,status,created_at"),
        @Index(name = "idx_training_session_knowledge_created", columnList = "knowledge_id,created_at")
    }
)
@Check(
    constraints = "total_questions >= 1 AND answered_questions >= 0 AND current_question_no >= 1 "
        + "AND (summary_score IS NULL OR (summary_score >= 0 AND summary_score <= 100))"
)
public class TrainingSession {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_training_session_user_id"))
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "knowledge_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_training_session_knowledge_id")
    )
    private Knowledge knowledge;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false, length = 20)
    private QuestionType questionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Difficulty difficulty;

    @Column(name = "hint_enabled", nullable = false)
    private Boolean hintEnabled;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TrainingSessionStatus status;

    @Column(name = "total_questions", nullable = false)
    private Integer totalQuestions;

    @Column(name = "answered_questions", nullable = false)
    private Integer answeredQuestions;

    @Column(name = "current_question_no", nullable = false)
    private Integer currentQuestionNo;

    @Column(name = "summary_score")
    private Integer summaryScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "summary_band", length = 20)
    private FeedbackBand summaryBand;

    @Column(name = "summary_major_issue", length = 255)
    private String summaryMajorIssue;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (hintEnabled == null) {
            hintEnabled = true;
        }
        if (status == null) {
            status = TrainingSessionStatus.IN_PROGRESS;
        }
        if (totalQuestions == null) {
            totalQuestions = 1;
        }
        if (answeredQuestions == null) {
            answeredQuestions = 0;
        }
        if (currentQuestionNo == null) {
            currentQuestionNo = 1;
        }
        if (startedAt == null) {
            startedAt = now;
        }
        if (createdAt == null) {
            createdAt = now;
        }
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

    public Knowledge getKnowledge() {
        return knowledge;
    }

    public void setKnowledge(Knowledge knowledge) {
        this.knowledge = knowledge;
    }

    public QuestionType getQuestionType() {
        return questionType;
    }

    public void setQuestionType(QuestionType questionType) {
        this.questionType = questionType;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    public Boolean getHintEnabled() {
        return hintEnabled;
    }

    public void setHintEnabled(Boolean hintEnabled) {
        this.hintEnabled = hintEnabled;
    }

    public TrainingSessionStatus getStatus() {
        return status;
    }

    public void setStatus(TrainingSessionStatus status) {
        this.status = status;
    }

    public Integer getTotalQuestions() {
        return totalQuestions;
    }

    public void setTotalQuestions(Integer totalQuestions) {
        this.totalQuestions = totalQuestions;
    }

    public Integer getAnsweredQuestions() {
        return answeredQuestions;
    }

    public void setAnsweredQuestions(Integer answeredQuestions) {
        this.answeredQuestions = answeredQuestions;
    }

    public Integer getCurrentQuestionNo() {
        return currentQuestionNo;
    }

    public void setCurrentQuestionNo(Integer currentQuestionNo) {
        this.currentQuestionNo = currentQuestionNo;
    }

    public Integer getSummaryScore() {
        return summaryScore;
    }

    public void setSummaryScore(Integer summaryScore) {
        this.summaryScore = summaryScore;
    }

    public FeedbackBand getSummaryBand() {
        return summaryBand;
    }

    public void setSummaryBand(FeedbackBand summaryBand) {
        this.summaryBand = summaryBand;
    }

    public String getSummaryMajorIssue() {
        return summaryMajorIssue;
    }

    public void setSummaryMajorIssue(String summaryMajorIssue) {
        this.summaryMajorIssue = summaryMajorIssue;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
