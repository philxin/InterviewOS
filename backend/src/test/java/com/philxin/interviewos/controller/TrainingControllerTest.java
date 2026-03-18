package com.philxin.interviewos.controller;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.philxin.interviewos.common.BusinessException;
import com.philxin.interviewos.common.GlobalExceptionHandler;
import com.philxin.interviewos.controller.dto.training.TrainingRecommendationItemResponse;
import com.philxin.interviewos.controller.dto.training.TrainingRecommendationResponse;
import com.philxin.interviewos.entity.AppUser;
import com.philxin.interviewos.entity.Knowledge;
import com.philxin.interviewos.entity.TrainingRecord;
import com.philxin.interviewos.llm.EvaluationResult;
import com.philxin.interviewos.security.AuthenticatedUser;
import com.philxin.interviewos.service.TrainingRecommendationService;
import com.philxin.interviewos.service.TrainingService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = TrainingController.class, properties = "server.servlet.context-path=")
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class TrainingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TrainingService trainingService;

    @MockBean
    private TrainingRecommendationService trainingRecommendationService;

    @Test
    void startTrainingReturns200() throws Exception {
        when(trainingService.startTraining(1L)).thenReturn("请解释 Spring Boot 自动配置原理。");

        String requestJson = objectMapper.writeValueAsString(new StartRequest(1L));
        mockMvc.perform(
            post("/training/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson)
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.question").value("请解释 Spring Boot 自动配置原理。"));
    }

    @Test
    void startTrainingValidationFailureReturns400() throws Exception {
        String requestJson = objectMapper.writeValueAsString(new StartRequest(0L));
        mockMvc.perform(
            post("/training/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson)
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").exists());

        verify(trainingService, never()).startTraining(anyLong());
    }

    @Test
    void submitAnswerReturns200() throws Exception {
        EvaluationResult evaluationResult = new EvaluationResult();
        evaluationResult.setAccuracy(80);
        evaluationResult.setDepth(70);
        evaluationResult.setClarity(90);
        evaluationResult.setOverall(80);
        evaluationResult.setStrengths("结构清晰");
        evaluationResult.setWeaknesses("细节偏少");
        evaluationResult.setSuggestions(List.of("补充条件注解机制"));
        evaluationResult.setExampleAnswer("示例回答");
        when(trainingService.submitAnswer(1L, "问题", "回答")).thenReturn(evaluationResult);

        String requestJson = objectMapper.writeValueAsString(new SubmitRequest(1L, "问题", "回答"));
        mockMvc.perform(
            post("/training/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson)
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.accuracy").value(80))
            .andExpect(jsonPath("$.data.overall").value(80))
            .andExpect(jsonPath("$.data.suggestions[0]").value("补充条件注解机制"))
            .andExpect(jsonPath("$.data.exampleAnswer").value("示例回答"));
    }

    @Test
    void getHistoryByKnowledgeIdReturns200() throws Exception {
        when(trainingService.getHistoryByKnowledgeId(1L)).thenReturn(List.of(buildRecord(11L, 1L)));

        mockMvc.perform(get("/training/history/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data[0].id").value(11))
            .andExpect(jsonPath("$.data[0].knowledgeId").value(1))
            .andExpect(jsonPath("$.data[0].suggestions[0]").value("补充边界条件"));
    }

    @Test
    void getAllHistoryReturns200() throws Exception {
        when(trainingService.getAllHistory()).thenReturn(List.of(buildRecord(22L, 2L)));

        mockMvc.perform(get("/training/history"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data[0].id").value(22))
            .andExpect(jsonPath("$.data[0].knowledgeId").value(2));
    }

    @Test
    void getHistoryWhenKnowledgeNotFoundReturns404() throws Exception {
        when(trainingService.getHistoryByKnowledgeId(9L))
            .thenThrow(new BusinessException(HttpStatus.NOT_FOUND, "Knowledge not found with id: 9"));

        mockMvc.perform(get("/training/history/9"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value(404))
            .andExpect(jsonPath("$.message").value("Knowledge not found with id: 9"))
            .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void getTodayRecommendationsReturns200() throws Exception {
        TrainingRecommendationItemResponse item1 = new TrainingRecommendationItemResponse();
        item1.setKnowledgeId(1L);
        item1.setQuestionType("SCENARIO");
        item1.setDifficulty("MEDIUM");

        TrainingRecommendationItemResponse item2 = new TrainingRecommendationItemResponse();
        item2.setKnowledgeId(2L);
        item2.setQuestionType("PROJECT");
        item2.setDifficulty("HARD");

        TrainingRecommendationResponse response = new TrainingRecommendationResponse();
        response.setPackageId("pkg_today_20260318_u1");
        response.setTitle("今日推荐练习");
        response.setItems(List.of(item1, item2));

        when(trainingRecommendationService.getTodayRecommendations(nullable(AuthenticatedUser.class))).thenReturn(response);

        mockMvc.perform(get("/training/recommendations/today").with(authentication(authenticationToken())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.packageId").value("pkg_today_20260318_u1"))
            .andExpect(jsonPath("$.data.title").value("今日推荐练习"))
            .andExpect(jsonPath("$.data.items[0].knowledgeId").value(1))
            .andExpect(jsonPath("$.data.items[0].questionType").value("SCENARIO"))
            .andExpect(jsonPath("$.data.items[1].difficulty").value("HARD"));
    }

    private TrainingRecord buildRecord(Long id, Long knowledgeId) {
        Knowledge knowledge = new Knowledge();
        knowledge.setId(knowledgeId);

        TrainingRecord record = new TrainingRecord();
        record.setId(id);
        record.setKnowledge(knowledge);
        record.setQuestion("问题");
        record.setAnswer("回答");
        record.setAccuracy(80);
        record.setDepth(75);
        record.setClarity(85);
        record.setOverall(80);
        record.setStrengths("优点");
        record.setWeaknesses("不足");
        record.setSuggestions("[\"补充边界条件\"]");
        record.setExampleAnswer("示例答案");
        record.setCreatedAt(LocalDateTime.of(2026, 3, 8, 0, 0, 0));
        return record;
    }

    private record StartRequest(Long knowledgeId) {
    }

    private record SubmitRequest(Long knowledgeId, String question, String answer) {
    }

    private UsernamePasswordAuthenticationToken authenticationToken() {
        return new UsernamePasswordAuthenticationToken(AuthenticatedUser.fromEntity(buildUser()), null);
    }

    private AppUser buildUser() {
        AppUser user = new AppUser();
        user.setId(1L);
        user.setEmail("user@example.com");
        user.setDisplayName("philxin");
        user.setPasswordHash("hash");
        return user;
    }
}
