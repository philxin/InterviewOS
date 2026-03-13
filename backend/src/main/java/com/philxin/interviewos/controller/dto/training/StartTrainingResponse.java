package com.philxin.interviewos.controller.dto.training;

public class StartTrainingResponse {
    private final String question;

    public StartTrainingResponse(String question) {
        this.question = question;
    }

    public String getQuestion() {
        return question;
    }
}
