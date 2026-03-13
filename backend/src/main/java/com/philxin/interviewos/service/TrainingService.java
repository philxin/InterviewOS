package com.philxin.interviewos.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.philxin.interviewos.common.BusinessException;
import com.philxin.interviewos.common.LogSanitizer;
import com.philxin.interviewos.entity.Knowledge;
import com.philxin.interviewos.entity.TrainingRecord;
import com.philxin.interviewos.llm.EvaluationResult;
import com.philxin.interviewos.llm.LLMService;
import com.philxin.interviewos.repository.KnowledgeRepository;
import com.philxin.interviewos.repository.TrainingRecordRepository;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TrainingService {
    private static final Logger log = LoggerFactory.getLogger(TrainingService.class);

    private final KnowledgeRepository knowledgeRepository;
    private final TrainingRecordRepository trainingRecordRepository;
    private final LLMService llmService;
    private final ObjectMapper objectMapper;

    public TrainingService(
        KnowledgeRepository knowledgeRepository,
        TrainingRecordRepository trainingRecordRepository,
        LLMService llmService,
        ObjectMapper objectMapper
    ) {
        this.knowledgeRepository = knowledgeRepository;
        this.trainingRecordRepository = trainingRecordRepository;
        this.llmService = llmService;
        this.objectMapper = objectMapper;
    }

    /**
     * 开始训练：按知识点生成一道问题。
     */
    @Transactional(readOnly = true)
    public String startTraining(Long knowledgeId) {
        Knowledge knowledge = getKnowledgeOrThrow(knowledgeId);
        log.info(
            "Start training: knowledgeId={}, titleFingerprint={}, contentLength={}",
            knowledgeId,
            LogSanitizer.fingerprint(knowledge.getTitle()),
            LogSanitizer.length(knowledge.getContent())
        );
        String question = llmService.generateQuestion(knowledge.getTitle(), knowledge.getContent());
        log.info(
            "Training question generated: knowledgeId={}, questionLength={}, questionFingerprint={}",
            knowledgeId,
            LogSanitizer.length(question),
            LogSanitizer.fingerprint(question)
        );
        return question;
    }

    /**
     * 提交回答：调用 LLM 评分，更新掌握度并持久化训练记录。
     */
    @Transactional
    public EvaluationResult submitAnswer(Long knowledgeId, String question, String answer) {
        Knowledge knowledge = getKnowledgeOrThrow(knowledgeId);
        String normalizedQuestion = normalize(question);
        String normalizedAnswer = normalize(answer);
        log.info(
            "Submit training answer: knowledgeId={}, questionLength={}, questionFingerprint={}, answerLength={}, answerFingerprint={}",
            knowledgeId,
            LogSanitizer.length(normalizedQuestion),
            LogSanitizer.fingerprint(normalizedQuestion),
            LogSanitizer.length(normalizedAnswer),
            LogSanitizer.fingerprint(normalizedAnswer)
        );

        EvaluationResult evaluation = llmService.evaluateAnswer(normalizedQuestion, normalizedAnswer);
        int accuracy = normalizeScore(evaluation.getAccuracy());
        int depth = normalizeScore(evaluation.getDepth());
        int clarity = normalizeScore(evaluation.getClarity());
        int overall = calculateOverall(accuracy, depth, clarity);

        List<String> suggestions = normalizeSuggestions(evaluation.getSuggestions());
        evaluation.setAccuracy(accuracy);
        evaluation.setDepth(depth);
        evaluation.setClarity(clarity);
        evaluation.setOverall(overall);
        evaluation.setSuggestions(suggestions);

        int newMastery = calculateMastery(knowledge.getMastery(), overall);
        knowledge.setMastery(newMastery);
        knowledgeRepository.save(knowledge);

        TrainingRecord record = new TrainingRecord();
        record.setKnowledge(knowledge);
        record.setQuestion(normalizedQuestion);
        record.setAnswer(normalizedAnswer);
        record.setAccuracy(accuracy);
        record.setDepth(depth);
        record.setClarity(clarity);
        record.setOverall(overall);
        record.setStrengths(normalize(evaluation.getStrengths()));
        record.setWeaknesses(normalize(evaluation.getWeaknesses()));
        record.setSuggestions(serializeSuggestions(suggestions));
        record.setExampleAnswer(normalize(evaluation.getExampleAnswer()));
        trainingRecordRepository.save(record);
        log.info(
            "Training evaluated: knowledgeId={}, overall={}, newMastery={}, suggestionsCount={}",
            knowledgeId,
            overall,
            newMastery,
            suggestions.size()
        );

        return evaluation;
    }

    /**
     * 查询指定知识点的训练历史（按时间倒序）。
     */
    @Transactional(readOnly = true)
    public List<TrainingRecord> getHistoryByKnowledgeId(Long knowledgeId) {
        getKnowledgeOrThrow(knowledgeId);
        List<TrainingRecord> records = trainingRecordRepository.findByKnowledgeIdOrderByCreatedAtDesc(knowledgeId);
        log.info("Query training history by knowledge: knowledgeId={}, recordCount={}", knowledgeId, records.size());
        return records;
    }

    /**
     * 查询全量训练历史（按时间倒序）。
     */
    @Transactional(readOnly = true)
    public List<TrainingRecord> getAllHistory() {
        List<TrainingRecord> records = trainingRecordRepository.findAllByOrderByCreatedAtDesc();
        log.info("Query all training history: recordCount={}", records.size());
        return records;
    }

    private Knowledge getKnowledgeOrThrow(Long knowledgeId) {
        return knowledgeRepository.findById(knowledgeId).orElseThrow(() -> notFound(knowledgeId));
    }

    private BusinessException notFound(Long id) {
        return new BusinessException(HttpStatus.NOT_FOUND, "Knowledge not found with id: " + id);
    }

    /**
     * V1 统一按三维平均分计算 overall，避免依赖模型返回漂移。
     */
    private int calculateOverall(int accuracy, int depth, int clarity) {
        return (accuracy + depth + clarity) / 3;
    }

    /**
     * 掌握度公式：newMastery = oldMastery * 0.7 + overall * 0.3。
     */
    private int calculateMastery(Integer oldMastery, int overall) {
        int base = oldMastery == null ? 0 : oldMastery;
        int mastery = (int) (base * 0.7 + overall * 0.3);
        return normalizeScore(mastery);
    }

    private int normalizeScore(Integer score) {
        int value = score == null ? 0 : score;
        return Math.max(0, Math.min(100, value));
    }

    private List<String> normalizeSuggestions(List<String> suggestions) {
        if (suggestions == null || suggestions.isEmpty()) {
            return List.of();
        }
        List<String> normalized = new ArrayList<>();
        for (String item : suggestions) {
            String value = normalize(item);
            if (!value.isBlank()) {
                normalized.add(value);
            }
        }
        return normalized;
    }

    private String serializeSuggestions(List<String> suggestions) {
        try {
            return objectMapper.writeValueAsString(suggestions == null ? List.of() : suggestions);
        } catch (JsonProcessingException exception) {
            log.error("Failed to serialize suggestions for training record", exception);
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to persist training suggestions");
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
