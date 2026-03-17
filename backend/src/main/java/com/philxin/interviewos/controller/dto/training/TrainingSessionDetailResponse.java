package com.philxin.interviewos.controller.dto.training;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 训练会话详情响应。
 */
public class TrainingSessionDetailResponse {
    private UUID sessionId;
    private Long knowledgeId;
    private String knowledgeTitle;
    private Integer questionCount;
    private Integer answeredCount;
    private Integer sessionScore;
    private FeedbackBandResponse band;
    private String majorIssueSummary;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private List<QuestionDetail> questions;

    public UUID getSessionId() {
        return sessionId;
    }

    public void setSessionId(UUID sessionId) {
        this.sessionId = sessionId;
    }

    public Long getKnowledgeId() {
        return knowledgeId;
    }

    public void setKnowledgeId(Long knowledgeId) {
        this.knowledgeId = knowledgeId;
    }

    public String getKnowledgeTitle() {
        return knowledgeTitle;
    }

    public void setKnowledgeTitle(String knowledgeTitle) {
        this.knowledgeTitle = knowledgeTitle;
    }

    public Integer getQuestionCount() {
        return questionCount;
    }

    public void setQuestionCount(Integer questionCount) {
        this.questionCount = questionCount;
    }

    public Integer getAnsweredCount() {
        return answeredCount;
    }

    public void setAnsweredCount(Integer answeredCount) {
        this.answeredCount = answeredCount;
    }

    public Integer getSessionScore() {
        return sessionScore;
    }

    public void setSessionScore(Integer sessionScore) {
        this.sessionScore = sessionScore;
    }

    public FeedbackBandResponse getBand() {
        return band;
    }

    public void setBand(FeedbackBandResponse band) {
        this.band = band;
    }

    public String getMajorIssueSummary() {
        return majorIssueSummary;
    }

    public void setMajorIssueSummary(String majorIssueSummary) {
        this.majorIssueSummary = majorIssueSummary;
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

    public List<QuestionDetail> getQuestions() {
        return questions;
    }

    public void setQuestions(List<QuestionDetail> questions) {
        this.questions = questions;
    }

    public static class QuestionDetail {
        private UUID questionId;
        private Integer orderNo;
        private UUID parentQuestionId;
        private String questionType;
        private String difficulty;
        private String question;
        private boolean hintAvailable;
        private String hintText;
        private boolean hintUsed;
        private String answer;
        private TrainingFeedbackResponse feedback;

        public UUID getQuestionId() {
            return questionId;
        }

        public void setQuestionId(UUID questionId) {
            this.questionId = questionId;
        }

        public Integer getOrderNo() {
            return orderNo;
        }

        public void setOrderNo(Integer orderNo) {
            this.orderNo = orderNo;
        }

        public UUID getParentQuestionId() {
            return parentQuestionId;
        }

        public void setParentQuestionId(UUID parentQuestionId) {
            this.parentQuestionId = parentQuestionId;
        }

        public String getQuestionType() {
            return questionType;
        }

        public void setQuestionType(String questionType) {
            this.questionType = questionType;
        }

        public String getDifficulty() {
            return difficulty;
        }

        public void setDifficulty(String difficulty) {
            this.difficulty = difficulty;
        }

        public String getQuestion() {
            return question;
        }

        public void setQuestion(String question) {
            this.question = question;
        }

        public boolean isHintAvailable() {
            return hintAvailable;
        }

        public void setHintAvailable(boolean hintAvailable) {
            this.hintAvailable = hintAvailable;
        }

        public String getHintText() {
            return hintText;
        }

        public void setHintText(String hintText) {
            this.hintText = hintText;
        }

        public boolean isHintUsed() {
            return hintUsed;
        }

        public void setHintUsed(boolean hintUsed) {
            this.hintUsed = hintUsed;
        }

        public String getAnswer() {
            return answer;
        }

        public void setAnswer(String answer) {
            this.answer = answer;
        }

        public TrainingFeedbackResponse getFeedback() {
            return feedback;
        }

        public void setFeedback(TrainingFeedbackResponse feedback) {
            this.feedback = feedback;
        }
    }
}
