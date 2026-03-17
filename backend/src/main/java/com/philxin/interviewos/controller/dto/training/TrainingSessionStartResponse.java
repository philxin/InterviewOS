package com.philxin.interviewos.controller.dto.training;

import java.util.UUID;

/**
 * 启动训练会话响应。
 */
public class TrainingSessionStartResponse {
    private UUID sessionId;
    private UUID questionId;
    private Long knowledgeId;
    private String knowledgeTitle;
    private String question;
    private String questionType;
    private String difficulty;
    private boolean hintAvailable;
    private Sequence sequence;

    public UUID getSessionId() {
        return sessionId;
    }

    public void setSessionId(UUID sessionId) {
        this.sessionId = sessionId;
    }

    public UUID getQuestionId() {
        return questionId;
    }

    public void setQuestionId(UUID questionId) {
        this.questionId = questionId;
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

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
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

    public boolean isHintAvailable() {
        return hintAvailable;
    }

    public void setHintAvailable(boolean hintAvailable) {
        this.hintAvailable = hintAvailable;
    }

    public Sequence getSequence() {
        return sequence;
    }

    public void setSequence(Sequence sequence) {
        this.sequence = sequence;
    }

    public static class Sequence {
        private int current;
        private int total;

        public Sequence() {
        }

        public Sequence(int current, int total) {
            this.current = current;
            this.total = total;
        }

        public int getCurrent() {
            return current;
        }

        public void setCurrent(int current) {
            this.current = current;
        }

        public int getTotal() {
            return total;
        }

        public void setTotal(int total) {
            this.total = total;
        }
    }
}
