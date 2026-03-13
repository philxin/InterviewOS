package com.philxin.interviewos.controller.dto.training;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.philxin.interviewos.entity.TrainingRecord;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TrainingRecordResponse {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private Long id;
    private Long knowledgeId;
    private String question;
    private String answer;
    private Integer accuracy;
    private Integer depth;
    private Integer clarity;
    private Integer overall;
    private String strengths;
    private String weaknesses;
    private List<String> suggestions;
    private String exampleAnswer;
    private LocalDateTime createdAt;

    public static TrainingRecordResponse fromEntity(TrainingRecord record) {
        TrainingRecordResponse response = new TrainingRecordResponse();
        response.setId(record.getId());
        response.setKnowledgeId(record.getKnowledge().getId());
        response.setQuestion(record.getQuestion());
        response.setAnswer(record.getAnswer());
        response.setAccuracy(record.getAccuracy());
        response.setDepth(record.getDepth());
        response.setClarity(record.getClarity());
        response.setOverall(record.getOverall());
        response.setStrengths(record.getStrengths());
        response.setWeaknesses(record.getWeaknesses());
        response.setSuggestions(parseSuggestions(record.getSuggestions()));
        response.setExampleAnswer(record.getExampleAnswer());
        response.setCreatedAt(record.getCreatedAt());
        return response;
    }

    /**
     * 兼容历史脏数据：优先解析 JSON 数组，失败时降级为单字符串数组。
     */
    private static List<String> parseSuggestions(String rawSuggestions) {
        if (rawSuggestions == null || rawSuggestions.isBlank()) {
            return List.of();
        }
        try {
            JsonNode node = OBJECT_MAPPER.readTree(rawSuggestions);
            if (node == null || node.isNull()) {
                return List.of();
            }
            if (node.isArray()) {
                List<String> suggestions = new ArrayList<>();
                Iterator<JsonNode> iterator = node.elements();
                while (iterator.hasNext()) {
                    String item = trim(iterator.next().asText(""));
                    if (!item.isEmpty()) {
                        suggestions.add(item);
                    }
                }
                return suggestions;
            }
            String single = trim(node.asText(""));
            return single.isEmpty() ? List.of() : List.of(single);
        } catch (Exception ignored) {
            String single = trim(rawSuggestions);
            return single.isEmpty() ? List.of() : List.of(single);
        }
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getKnowledgeId() {
        return knowledgeId;
    }

    public void setKnowledgeId(Long knowledgeId) {
        this.knowledgeId = knowledgeId;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
