package com.philxin.interviewos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.philxin.interviewos.common.BusinessException;
import com.philxin.interviewos.entity.Knowledge;
import com.philxin.interviewos.entity.TrainingRecord;
import com.philxin.interviewos.llm.EvaluationResult;
import com.philxin.interviewos.llm.LLMService;
import com.philxin.interviewos.repository.KnowledgeRepository;
import com.philxin.interviewos.repository.TrainingRecordRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class TrainingServiceTest {

    @Mock
    private KnowledgeRepository knowledgeRepository;

    @Mock
    private TrainingRecordRepository trainingRecordRepository;

    @Mock
    private LLMService llmService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private TrainingService trainingService;

    @BeforeEach
    void setUp() {
        trainingService = new TrainingService(knowledgeRepository, trainingRecordRepository, llmService, objectMapper);
    }

    @Test
    void submitAnswerUpdatesMasteryAndPersistsTrainingRecord() throws Exception {
        Knowledge knowledge = buildKnowledge(1L, 50);
        when(knowledgeRepository.findById(1L)).thenReturn(Optional.of(knowledge));
        when(knowledgeRepository.save(any(Knowledge.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(trainingRecordRepository.save(any(TrainingRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EvaluationResult llmResult = new EvaluationResult();
        llmResult.setAccuracy(80);
        llmResult.setDepth(70);
        llmResult.setClarity(90);
        llmResult.setOverall(5);
        llmResult.setStrengths("优点");
        llmResult.setWeaknesses("不足");
        llmResult.setSuggestions(List.of(" 补充示例 ", ""));
        llmResult.setExampleAnswer(" 示例回答 ");
        when(llmService.evaluateAnswer("问题", "回答")).thenReturn(llmResult);

        EvaluationResult result = trainingService.submitAnswer(1L, " 问题 ", " 回答 ");
        assertEquals(80, result.getOverall());
        assertEquals(List.of("补充示例"), result.getSuggestions());
        assertEquals(59, knowledge.getMastery());

        ArgumentCaptor<TrainingRecord> recordCaptor = ArgumentCaptor.forClass(TrainingRecord.class);
        verify(trainingRecordRepository).save(recordCaptor.capture());
        TrainingRecord saved = recordCaptor.getValue();
        assertEquals("问题", saved.getQuestion());
        assertEquals("回答", saved.getAnswer());

        List<String> suggestions = objectMapper.readValue(saved.getSuggestions(), new TypeReference<>() {
        });
        assertEquals(List.of("补充示例"), suggestions);
    }

    @Test
    void startTrainingThrows404WhenKnowledgeNotFound() {
        when(knowledgeRepository.findById(99L)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class, () -> trainingService.startTraining(99L));
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertEquals("Knowledge not found with id: 99", exception.getMessage());
    }

    @Test
    void getHistoryByKnowledgeIdThrows404WhenKnowledgeNotFound() {
        when(knowledgeRepository.findById(88L)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> trainingService.getHistoryByKnowledgeId(88L)
        );
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    void getHistoryByKnowledgeIdReturnsRepositoryResult() {
        Knowledge knowledge = buildKnowledge(7L, 40);
        TrainingRecord record = new TrainingRecord();
        record.setId(1L);
        record.setKnowledge(knowledge);
        when(knowledgeRepository.findById(7L)).thenReturn(Optional.of(knowledge));
        when(trainingRecordRepository.findByKnowledgeIdOrderByCreatedAtDesc(7L)).thenReturn(List.of(record));

        List<TrainingRecord> history = trainingService.getHistoryByKnowledgeId(7L);
        assertEquals(1, history.size());
        assertEquals(1L, history.get(0).getId());
    }

    @Test
    void getAllHistoryReturnsRepositoryResult() {
        when(trainingRecordRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(new TrainingRecord()));

        List<TrainingRecord> records = trainingService.getAllHistory();
        assertEquals(1, records.size());
    }

    @Test
    void submitAnswerClampsOutOfRangeScores() {
        Knowledge knowledge = buildKnowledge(5L, 0);
        when(knowledgeRepository.findById(5L)).thenReturn(Optional.of(knowledge));
        when(knowledgeRepository.save(any(Knowledge.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(trainingRecordRepository.save(any(TrainingRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EvaluationResult llmResult = new EvaluationResult();
        llmResult.setAccuracy(200);
        llmResult.setDepth(-10);
        llmResult.setClarity(50);
        llmResult.setSuggestions(List.of());
        when(llmService.evaluateAnswer("Q", "A")).thenReturn(llmResult);

        EvaluationResult result = trainingService.submitAnswer(5L, "Q", "A");
        assertEquals(100, result.getAccuracy());
        assertEquals(0, result.getDepth());
        assertEquals(50, result.getClarity());
        assertEquals(50, result.getOverall());
        assertTrue(result.getSuggestions().isEmpty());
    }

    private Knowledge buildKnowledge(Long id, Integer mastery) {
        Knowledge knowledge = new Knowledge();
        knowledge.setId(id);
        knowledge.setTitle("title");
        knowledge.setContent("content");
        knowledge.setMastery(mastery);
        return knowledge;
    }
}
