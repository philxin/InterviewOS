package com.philxin.interviewos.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.philxin.interviewos.common.BusinessException;
import com.philxin.interviewos.common.GlobalExceptionHandler;
import com.philxin.interviewos.controller.dto.training.FeedbackBandResponse;
import com.philxin.interviewos.controller.dto.training.TrainingFeedbackResponse;
import com.philxin.interviewos.controller.dto.training.TrainingHintResponse;
import com.philxin.interviewos.controller.dto.training.TrainingSessionDetailResponse;
import com.philxin.interviewos.controller.dto.training.TrainingSessionListResponse;
import com.philxin.interviewos.controller.dto.training.TrainingSessionStartResponse;
import com.philxin.interviewos.entity.AppUser;
import com.philxin.interviewos.security.AuthenticatedUser;
import com.philxin.interviewos.service.TrainingSessionService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
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

@WebMvcTest(controllers = TrainingSessionController.class, properties = "server.servlet.context-path=")
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class TrainingSessionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TrainingSessionService trainingSessionService;

    @Test
    void startSessionReturns200() throws Exception {
        TrainingSessionStartResponse response = new TrainingSessionStartResponse();
        response.setSessionId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        response.setQuestionId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
        response.setKnowledgeId(1L);
        response.setKnowledgeTitle("Spring");
        response.setQuestion("请解释自动配置。");
        response.setQuestionType("PROJECT");
        response.setDifficulty("MEDIUM");
        response.setHintAvailable(true);
        response.setSequence(new TrainingSessionStartResponse.Sequence(1, 1));
        when(trainingSessionService.startSession(nullable(AuthenticatedUser.class), org.mockito.ArgumentMatchers.any()))
            .thenReturn(response);

        String requestJson = objectMapper.writeValueAsString(new StartRequest(1L, "PROJECT", "MEDIUM", true));
        mockMvc.perform(
            post("/training/sessions")
                .with(authentication(authenticationToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson)
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.sessionId").value("11111111-1111-1111-1111-111111111111"))
            .andExpect(jsonPath("$.data.questionType").value("PROJECT"));
    }

    @Test
    void submitAnswerReturns200() throws Exception {
        TrainingFeedbackResponse response = new TrainingFeedbackResponse();
        FeedbackBandResponse band = new FeedbackBandResponse();
        band.setCode("GOOD");
        band.setLabel("回答较完整");
        band.setDescription("结构较清晰，关键点基本覆盖。");
        response.setScore(72);
        response.setBand(band);
        response.setMajorIssue("细节还能加强");
        response.setMissingPoints(List.of("补充项目细节"));
        response.setBetterAnswerApproach(List.of("先讲原理再讲例子"));
        response.setNaturalExampleAnswer("示例");
        response.setWeakTags(List.of("spring"));
        response.setMasteryBefore(40);
        response.setMasteryAfter(49);
        when(trainingSessionService.submitAnswer(
            nullable(AuthenticatedUser.class),
            eq(UUID.fromString("11111111-1111-1111-1111-111111111111")),
            org.mockito.ArgumentMatchers.any()
        )).thenReturn(response);

        String requestJson = objectMapper.writeValueAsString(
            new SubmitRequest("22222222-2222-2222-2222-222222222222", "回答")
        );
        mockMvc.perform(
            post("/training/sessions/11111111-1111-1111-1111-111111111111/answers")
                .with(authentication(authenticationToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson)
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.score").value(72))
            .andExpect(jsonPath("$.data.band.code").value("GOOD"))
            .andExpect(jsonPath("$.data.masteryAfter").value(49));
    }

    @Test
    void getHintReturns200() throws Exception {
        TrainingHintResponse response = new TrainingHintResponse();
        response.setHint("可以先讲入口，再讲条件装配。");
        when(trainingSessionService.getHint(
            nullable(AuthenticatedUser.class),
            eq(UUID.fromString("11111111-1111-1111-1111-111111111111")),
            eq(UUID.fromString("22222222-2222-2222-2222-222222222222"))
        )).thenReturn(response);

        mockMvc.perform(
            post("/training/sessions/11111111-1111-1111-1111-111111111111/questions/22222222-2222-2222-2222-222222222222/hint")
                .with(authentication(authenticationToken()))
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.hint").value("可以先讲入口，再讲条件装配。"));
    }

    @Test
    void getSessionHistoryReturns200() throws Exception {
        TrainingSessionSummaryResponseItem item = new TrainingSessionSummaryResponseItem();
        TrainingSessionListResponse response = new TrainingSessionListResponse();
        response.setItems(List.of(item.toResponse()));
        response.setPage(1);
        response.setSize(20);
        response.setTotal(1);
        response.setHasNext(false);
        when(trainingSessionService.getSessionHistory(nullable(AuthenticatedUser.class), eq(1L), eq(1), eq(20)))
            .thenReturn(response);

        mockMvc.perform(
            get("/training/sessions")
                .with(authentication(authenticationToken()))
                .param("knowledgeId", "1")
                .param("page", "1")
                .param("size", "20")
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[0].knowledgeId").value(1))
            .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void getSessionDetailReturns200() throws Exception {
        TrainingSessionDetailResponse response = new TrainingSessionDetailResponse();
        response.setSessionId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        response.setKnowledgeId(1L);
        response.setKnowledgeTitle("Spring");
        response.setQuestionCount(1);
        response.setAnsweredCount(1);
        response.setStartedAt(LocalDateTime.of(2026, 3, 8, 0, 0));
        response.setCompletedAt(LocalDateTime.of(2026, 3, 8, 0, 3));
        TrainingSessionDetailResponse.QuestionDetail question = new TrainingSessionDetailResponse.QuestionDetail();
        question.setQuestionId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
        question.setOrderNo(1);
        question.setQuestion("问题");
        question.setQuestionType("PROJECT");
        question.setDifficulty("MEDIUM");
        response.setQuestions(List.of(question));
        when(trainingSessionService.getSessionDetail(
            nullable(AuthenticatedUser.class),
            eq(UUID.fromString("11111111-1111-1111-1111-111111111111"))
        )).thenReturn(response);

        mockMvc.perform(
            get("/training/sessions/11111111-1111-1111-1111-111111111111")
                .with(authentication(authenticationToken()))
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.questions[0].question").value("问题"));
    }

    @Test
    void getSessionDetailNotFoundReturns404() throws Exception {
        when(trainingSessionService.getSessionDetail(
            nullable(AuthenticatedUser.class),
            eq(UUID.fromString("11111111-1111-1111-1111-111111111111"))
        )).thenThrow(new BusinessException(HttpStatus.NOT_FOUND, "Training session not found"));

        mockMvc.perform(
            get("/training/sessions/11111111-1111-1111-1111-111111111111")
                .with(authentication(authenticationToken()))
        )
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value(404))
            .andExpect(jsonPath("$.message").value("Training session not found"));
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

    private record StartRequest(Long knowledgeId, String questionType, String difficulty, Boolean hintEnabled) {
    }

    private record SubmitRequest(String questionId, String answer) {
    }

    private static class TrainingSessionSummaryResponseItem {
        com.philxin.interviewos.controller.dto.training.TrainingSessionSummaryResponse toResponse() {
            com.philxin.interviewos.controller.dto.training.TrainingSessionSummaryResponse response =
                new com.philxin.interviewos.controller.dto.training.TrainingSessionSummaryResponse();
            response.setSessionId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
            response.setKnowledgeId(1L);
            response.setKnowledgeTitle("Spring");
            response.setQuestionCount(1);
            response.setAnsweredCount(1);
            response.setSessionScore(72);
            response.setStartedAt(LocalDateTime.of(2026, 3, 8, 0, 0));
            response.setCompletedAt(LocalDateTime.of(2026, 3, 8, 0, 3));
            return response;
        }
    }
}
