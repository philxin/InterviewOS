package com.philxin.interviewos.controller.dto.training;

import com.philxin.interviewos.llm.EvaluationResult;
import java.util.List;

public class SubmitTrainingResponse {
    private Integer accuracy;
    private Integer depth;
    private Integer clarity;
    private Integer overall;
    private String strengths;
    private String weaknesses;
    private List<String> suggestions;
    private String exampleAnswer;

    public static SubmitTrainingResponse fromEvaluationResult(EvaluationResult evaluationResult) {
        SubmitTrainingResponse response = new SubmitTrainingResponse();
        response.setAccuracy(evaluationResult.getAccuracy());
        response.setDepth(evaluationResult.getDepth());
        response.setClarity(evaluationResult.getClarity());
        response.setOverall(evaluationResult.getOverall());
        response.setStrengths(evaluationResult.getStrengths());
        response.setWeaknesses(evaluationResult.getWeaknesses());
        response.setSuggestions(evaluationResult.getSuggestions());
        response.setExampleAnswer(evaluationResult.getExampleAnswer());
        return response;
    }

    public Integer getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(Integer accuracy) {
        this.accuracy = accuracy;
    }

    public Integer getDepth() {
        return depth;
    }

    public void setDepth(Integer depth) {
        this.depth = depth;
    }

    public Integer getClarity() {
        return clarity;
    }

    public void setClarity(Integer clarity) {
        this.clarity = clarity;
    }

    public Integer getOverall() {
        return overall;
    }

    public void setOverall(Integer overall) {
        this.overall = overall;
    }

    public String getStrengths() {
        return strengths;
    }

    public void setStrengths(String strengths) {
        this.strengths = strengths;
    }

    public String getWeaknesses() {
        return weaknesses;
    }

    public void setWeaknesses(String weaknesses) {
        this.weaknesses = weaknesses;
    }

    public List<String> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(List<String> suggestions) {
        this.suggestions = suggestions;
    }

    public String getExampleAnswer() {
        return exampleAnswer;
    }

    public void setExampleAnswer(String exampleAnswer) {
        this.exampleAnswer = exampleAnswer;
    }
}
