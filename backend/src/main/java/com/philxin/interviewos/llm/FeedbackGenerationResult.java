package com.philxin.interviewos.llm;

import java.util.ArrayList;
import java.util.List;

/**
 * V2 反馈生成结果，面向训练会话主链路消费。
 */
public class FeedbackGenerationResult {
    private Integer score;
    private String majorIssue;
    private List<String> missingPoints;
    private List<String> betterAnswerApproach;
    private String naturalExampleAnswer;

    public FeedbackGenerationResult() {
        this.missingPoints = new ArrayList<>();
        this.betterAnswerApproach = new ArrayList<>();
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public String getMajorIssue() {
        return majorIssue;
    }

    public void setMajorIssue(String majorIssue) {
        this.majorIssue = majorIssue;
    }

    public List<String> getMissingPoints() {
        return missingPoints;
    }

    public void setMissingPoints(List<String> missingPoints) {
        this.missingPoints = missingPoints == null ? new ArrayList<>() : missingPoints;
    }

    public List<String> getBetterAnswerApproach() {
        return betterAnswerApproach;
    }

    public void setBetterAnswerApproach(List<String> betterAnswerApproach) {
        this.betterAnswerApproach = betterAnswerApproach == null ? new ArrayList<>() : betterAnswerApproach;
    }

    public String getNaturalExampleAnswer() {
        return naturalExampleAnswer;
    }

    public void setNaturalExampleAnswer(String naturalExampleAnswer) {
        this.naturalExampleAnswer = naturalExampleAnswer;
    }
}
