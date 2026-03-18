package com.philxin.interviewos.llm;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PromptTemplateTest {

    @Test
    void questionPromptTruncatesOverlongKnowledgeContent() {
        String prompt = PromptTemplate.questionGenerationPrompt("Redis", "x".repeat(6000));

        assertTrue(prompt.contains("知识点标题：\nRedis"));
        assertFalse(prompt.contains("x".repeat(4000)));
        assertTrue(prompt.contains("..."));
    }

    @Test
    void feedbackPromptTruncatesOverlongAnswer() {
        FeedbackGenerationRequest request = new FeedbackGenerationRequest();
        request.setKnowledgeTitle("JVM");
        request.setKnowledgeContent("content");
        request.setQuestionType("FUNDAMENTAL");
        request.setDifficulty("MEDIUM");
        request.setQuestionText("请解释 JVM 内存模型");
        request.setUserAnswer("y".repeat(8000));

        String prompt = PromptTemplate.feedbackGenerationPrompt(request);

        assertFalse(prompt.contains("y".repeat(5000)));
        assertTrue(prompt.contains("..."));
    }
}
