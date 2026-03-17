package com.philxin.interviewos.controller.dto.training;

import java.util.List;

/**
 * 训练反馈响应。
 */
public class TrainingFeedbackResponse {
    private Integer score;
    private FeedbackBandResponse band;
    private String majorIssue;
    private List<String> missingPoints;
    private List<String> betterAnswerApproach;
    private String naturalExampleAnswer;
    private List<String> weakTags;
    private Integer masteryBefore;
    private Integer masteryAfter;

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public FeedbackBandResponse getBand() {
        return band;
    }

    public void setBand(FeedbackBandResponse band) {
        this.band = band;
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
        this.missingPoints = missingPoints;
    }

    public List<String> getBetterAnswerApproach() {
        return betterAnswerApproach;
    }

    public void setBetterAnswerApproach(List<String> betterAnswerApproach) {
        this.betterAnswerApproach = betterAnswerApproach;
    }

    public String getNaturalExampleAnswer() {
        return naturalExampleAnswer;
    }

    public void setNaturalExampleAnswer(String naturalExampleAnswer) {
        this.naturalExampleAnswer = naturalExampleAnswer;
    }

    public List<String> getWeakTags() {
        return weakTags;
    }

    public void setWeakTags(List<String> weakTags) {
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
}
