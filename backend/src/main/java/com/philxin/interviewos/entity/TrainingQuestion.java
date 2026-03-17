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
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.UUID;
import org.hibernate.annotations.Check;

/**
 * V2 训练问题事实表。
 */
@Entity
@Table(
    name = "training_question",
    indexes = {
        @Index(name = "idx_training_question_knowledge_created", columnList = "knowledge_id,created_at"),
        @Index(name = "idx_training_question_band_created", columnList = "feedback_band,created_at")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_training_question_session_order", columnNames = {"session_id", "order_no"})
    }
)
@Check(
    constraints = "order_no >= 1 "
        + "AND (score IS NULL OR (score >= 0 AND score <= 100)) "
        + "AND (mastery_before IS NULL OR (mastery_before >= 0 AND mastery_before <= 100)) "
        + "AND (mastery_after IS NULL OR (mastery_after >= 0 AND mastery_after <= 100))"
)
public class TrainingQuestion {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "session_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_training_question_session_id")
    )
    private TrainingSession session;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "knowledge_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_training_question_knowledge_id")
    )
    private Knowledge knowledge;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "parent_question_id",
        foreignKey = @ForeignKey(name = "fk_training_question_parent_id")
    )
    private TrainingQuestion parentQuestion;

    @Column(name = "order_no", nullable = false)
    private Integer orderNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false, length = 20)
    private QuestionType questionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Difficulty difficulty;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Column(name = "hint_text", columnDefinition = "TEXT")
    private String hintText;

    @Column(name = "hint_used", nullable = false)
    private Boolean hintUsed;

    @Column(name = "answer_text", columnDefinition = "TEXT")
    private String answerText;

    @Column
    private Integer score;

    @Enumerated(EnumType.STRING)
    @Column(name = "feedback_band", length = 20)
    private FeedbackBand feedbackBand;

    @Column(name = "major_issue", length = 255)
    private String majorIssue;

    @Column(name = "missing_points", columnDefinition = "TEXT")
    private String missingPoints;

    @Column(name = "better_answer_approach", columnDefinition = "TEXT")
    private String betterAnswerApproach;

    @Column(name = "natural_example_answer", columnDefinition = "TEXT")
    private String naturalExampleAnswer;

    @Column(name = "weak_tags", columnDefinition = "TEXT")
    private String weakTags;

    @Column(name = "mastery_before")
    private Integer masteryBefore;

    @Column(name = "mastery_after")
    private Integer masteryAfter;

    @Column(name = "answered_at")
    private LocalDateTime answeredAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (hintUsed == null) {
            hintUsed = false;
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public TrainingSession getSession() {
        return session;
    }

    public void setSession(TrainingSession session) {
        this.session = session;
    }

    public Knowledge getKnowledge() {
        return knowledge;
    }

    public void setKnowledge(Knowledge knowledge) {
        this.knowledge = knowledge;
    }

    public TrainingQuestion getParentQuestion() {
        return parentQuestion;
    }

    public void setParentQuestion(TrainingQuestion parentQuestion) {
        this.parentQuestion = parentQuestion;
    }

    public Integer getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(Integer orderNo) {
        this.orderNo = orderNo;
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

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public String getHintText() {
        return hintText;
    }

    public void setHintText(String hintText) {
        this.hintText = hintText;
    }

    public Boolean getHintUsed() {
        return hintUsed;
    }

    public void setHintUsed(Boolean hintUsed) {
        this.hintUsed = hintUsed;
    }

    public String getAnswerText() {
        return answerText;
    }

    public void setAnswerText(String answerText) {
        this.answerText = answerText;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public FeedbackBand getFeedbackBand() {
        return feedbackBand;
    }

    public void setFeedbackBand(FeedbackBand feedbackBand) {
        this.feedbackBand = feedbackBand;
    }

    public String getMajorIssue() {
        return majorIssue;
    }

    public void setMajorIssue(String majorIssue) {
        this.majorIssue = majorIssue;
    }

    public String getMissingPoints() {
        return missingPoints;
    }

    public void setMissingPoints(String missingPoints) {
        this.missingPoints = missingPoints;
    }

    public String getBetterAnswerApproach() {
        return betterAnswerApproach;
    }

    public void setBetterAnswerApproach(String betterAnswerApproach) {
        this.betterAnswerApproach = betterAnswerApproach;
    }

    public String getNaturalExampleAnswer() {
        return naturalExampleAnswer;
    }

    public void setNaturalExampleAnswer(String naturalExampleAnswer) {
        this.naturalExampleAnswer = naturalExampleAnswer;
    }

    public String getWeakTags() {
        return weakTags;
    }

    public void setWeakTags(String weakTags) {
        this.weakTags = weakTags;
    }

    public Integer getMasteryBefore() {
        return masteryBefore;
    }

    public void setMasteryBefore(Integer masteryBefore) {
        this.masteryBefore = masteryBefore;
    }

    public Integer getMasteryAfter() {
        return masteryAfter;
    }

    public void setMasteryAfter(Integer masteryAfter) {
        this.masteryAfter = masteryAfter;
    }

    public LocalDateTime getAnsweredAt() {
        return answeredAt;
    }

    public void setAnsweredAt(LocalDateTime answeredAt) {
        this.answeredAt = answeredAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
