package com.philxin.interviewos.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.philxin.interviewos.common.BusinessException;
import com.philxin.interviewos.common.LogSanitizer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class LLMServiceImpl implements LLMService {
    private static final Logger log = LoggerFactory.getLogger(LLMServiceImpl.class);

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    public LLMServiceImpl(ChatClient.Builder chatClientBuilder, ObjectMapper objectMapper) {
        this.chatClient = chatClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    /**
     * 基于知识点生成单题，失败时映射为 502。
     */
    @Override
    public String generateQuestion(String knowledgeTitle, String knowledgeContent) {
        String prompt = PromptTemplate.questionGenerationPrompt(knowledgeTitle, knowledgeContent);
        String response = callModel(prompt, "generateQuestion");
        if (response.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "LLM did not return question content");
        }
        return response.trim();
    }

    /**
     * 生成答题提示，失败时映射为 502。
     */
    @Override
    public String generateHint(String knowledgeTitle, String knowledgeContent, String questionText) {
        String prompt = PromptTemplate.hintGenerationPrompt(knowledgeTitle, knowledgeContent, questionText);
        String response = callModel(prompt, "generateHint");
        if (response.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "LLM did not return hint content");
        }
        return response.trim();
    }

    /**
     * 评估回答并解析为结构化结果，解析失败时映射为 502。
     */
    @Override
    public EvaluationResult evaluateAnswer(String question, String userAnswer) {
        String prompt = PromptTemplate.answerEvaluationPrompt(question, userAnswer);
        String rawResponse = callModel(prompt, "evaluateAnswer");
        return parseEvaluationResult(rawResponse);
    }

    /**
     * 按 V2 反馈结构评估回答，解析失败时映射为 502。
     */
    @Override
    public FeedbackGenerationResult evaluateAnswer(FeedbackGenerationRequest request) {
        String prompt = PromptTemplate.feedbackGenerationPrompt(request);
        String rawResponse = callModel(prompt, "evaluateAnswerV2");
        return parseFeedbackGenerationResult(rawResponse);
    }

    private String callModel(String prompt, String operation) {
        long start = System.currentTimeMillis();
        String promptFingerprint = LogSanitizer.fingerprint(prompt);
        int promptLength = LogSanitizer.length(prompt);
        try {
            String content = chatClient.prompt(prompt).call().content();
            long elapsed = System.currentTimeMillis() - start;
            log.info(
                "LLM {} completed in {} ms, promptLength={}, promptFingerprint={}, responseLength={}, responseFingerprint={}",
                operation,
                elapsed,
                promptLength,
                promptFingerprint,
                LogSanitizer.length(content),
                LogSanitizer.fingerprint(content)
            );
            return content == null ? "" : content;
        } catch (Exception exception) {
            long elapsed = System.currentTimeMillis() - start;
            log.error(
                "LLM {} failed in {} ms, promptLength={}, promptFingerprint={}",
                operation,
                elapsed,
                promptLength,
                promptFingerprint,
                exception
            );
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "LLM service invocation failed");
        }
    }

    private EvaluationResult parseEvaluationResult(String rawResponse) {
        try {
            String json = normalizeJson(rawResponse);
            JsonNode root = objectMapper.readTree(json);
            EvaluationResult result = new EvaluationResult();
            result.setAccuracy(normalizeScore(root.path("accuracy").asInt(0)));
            result.setDepth(normalizeScore(root.path("depth").asInt(0)));
            result.setClarity(normalizeScore(root.path("clarity").asInt(0)));
            int overall = root.has("overall")
                ? normalizeScore(root.path("overall").asInt(0))
                : normalizeScore((result.getAccuracy() + result.getDepth() + result.getClarity()) / 3);
            result.setOverall(overall);
            result.setStrengths(root.path("strengths").asText(""));
            result.setWeaknesses(root.path("weaknesses").asText(""));
            result.setSuggestions(parseSuggestions(root.path("suggestions")));
            result.setExampleAnswer(readExampleAnswer(root));
            return result;
        } catch (JsonProcessingException exception) {
            log.error(
                "Failed to parse LLM evaluation response: responseLength={}, responseFingerprint={}",
                LogSanitizer.length(rawResponse),
                LogSanitizer.fingerprint(rawResponse),
                exception
            );
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "LLM returned invalid evaluation format");
        }
    }

    private FeedbackGenerationResult parseFeedbackGenerationResult(String rawResponse) {
        try {
            String json = normalizeJson(rawResponse);
            JsonNode root = objectMapper.readTree(json);
            FeedbackGenerationResult result = new FeedbackGenerationResult();
            result.setScore(resolveFeedbackScore(root));
            result.setMajorIssue(readText(root, "majorIssue", "major_issue", "weaknesses"));
            result.setMissingPoints(parseStringList(root, "missingPoints", "missing_points", "suggestions"));
            result.setBetterAnswerApproach(
                parseStringList(root, "betterAnswerApproach", "better_answer_approach", "suggestions")
            );
            result.setNaturalExampleAnswer(
                readText(
                    root,
                    "naturalExampleAnswer",
                    "natural_example_answer",
                    "exampleAnswer",
                    "example_answer"
                )
            );
            return result;
        } catch (JsonProcessingException exception) {
            log.error(
                "Failed to parse LLM V2 feedback response: responseLength={}, responseFingerprint={}",
                LogSanitizer.length(rawResponse),
                LogSanitizer.fingerprint(rawResponse),
                exception
            );
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "LLM returned invalid V2 feedback format");
        }
    }

    private String normalizeJson(String rawResponse) {
        if (rawResponse == null) {
            return "";
        }
        String trimmed = rawResponse.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```json\\s*", "");
            trimmed = trimmed.replaceFirst("^```\\s*", "");
            trimmed = trimmed.replaceFirst("\\s*```$", "");
        }
        return trimmed.trim();
    }

    private int normalizeScore(int score) {
        return Math.max(0, Math.min(100, score));
    }

    private List<String> parseSuggestions(JsonNode suggestionsNode) {
        List<String> suggestions = new ArrayList<>();
        if (suggestionsNode == null || suggestionsNode.isNull()) {
            return suggestions;
        }
        if (suggestionsNode.isArray()) {
            Iterator<JsonNode> iterator = suggestionsNode.elements();
            while (iterator.hasNext()) {
                String item = iterator.next().asText("").trim();
                if (!item.isEmpty()) {
                    suggestions.add(item);
                }
            }
            return suggestions;
        }
        String single = suggestionsNode.asText("").trim();
        if (!single.isEmpty()) {
            suggestions.add(single);
        }
        return suggestions;
    }

    private List<String> parseStringList(JsonNode root, String... fieldNames) {
        for (String fieldName : fieldNames) {
            if (root.has(fieldName)) {
                return parseSuggestions(root.path(fieldName));
            }
        }
        return List.of();
    }

    private String readText(JsonNode root, String... fieldNames) {
        for (String fieldName : fieldNames) {
            if (root.has(fieldName)) {
                return root.path(fieldName).asText("");
            }
        }
        return "";
    }

    private int resolveFeedbackScore(JsonNode root) {
        if (root.has("score")) {
            return normalizeScore(root.path("score").asInt(0));
        }
        if (root.has("overall")) {
            return normalizeScore(root.path("overall").asInt(0));
        }
        int accuracy = normalizeScore(root.path("accuracy").asInt(0));
        int depth = normalizeScore(root.path("depth").asInt(0));
        int clarity = normalizeScore(root.path("clarity").asInt(0));
        return normalizeScore((accuracy + depth + clarity) / 3);
    }

    private String readExampleAnswer(JsonNode root) {
        if (root.has("exampleAnswer")) {
            return root.path("exampleAnswer").asText("");
        }
        return root.path("example_answer").asText("");
    }
}
