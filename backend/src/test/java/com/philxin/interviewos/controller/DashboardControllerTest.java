package com.philxin.interviewos.controller;

import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.philxin.interviewos.common.GlobalExceptionHandler;
import com.philxin.interviewos.controller.dto.dashboard.DashboardOverviewResponse;
import com.philxin.interviewos.controller.dto.dashboard.ProgressSummaryResponse;
import com.philxin.interviewos.controller.dto.dashboard.RecentTrainingItemResponse;
import com.philxin.interviewos.controller.dto.dashboard.ReviewReminderItemResponse;
import com.philxin.interviewos.controller.dto.dashboard.ReviewReminderResponse;
import com.philxin.interviewos.controller.dto.dashboard.WeakKnowledgeItemResponse;
import com.philxin.interviewos.controller.dto.training.FeedbackBandResponse;
import com.philxin.interviewos.entity.AppUser;
import com.philxin.interviewos.security.AuthenticatedUser;
import com.philxin.interviewos.service.DashboardService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = DashboardController.class, properties = "server.servlet.context-path=")
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DashboardService dashboardService;

    @Test
    void getOverviewReturns200() throws Exception {
        DashboardOverviewResponse response = new DashboardOverviewResponse();
        WeakKnowledgeItemResponse weakItem = new WeakKnowledgeItemResponse();
        weakItem.setKnowledgeId(1L);
        weakItem.setTitle("Spring");
        weakItem.setMastery(42);
        weakItem.setTags(List.of("spring"));
        response.setWeakKnowledgeItems(List.of(weakItem));

        RecentTrainingItemResponse trainingItem = new RecentTrainingItemResponse();
        trainingItem.setSessionId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        trainingItem.setKnowledgeId(1L);
        trainingItem.setKnowledgeTitle("Spring");
        trainingItem.setSessionScore(58);
        FeedbackBandResponse band = new FeedbackBandResponse();
        band.setCode("BASIC");
        band.setLabel("基础尚可");
        trainingItem.setBand(band);
        trainingItem.setCompletedAt(LocalDateTime.of(2026, 3, 17, 9, 0));
        response.setRecentTrainings(List.of(trainingItem));

        ProgressSummaryResponse progressSummary = new ProgressSummaryResponse();
        progressSummary.setTrainedCountLast7Days(3);
        progressSummary.setAverageScoreLast7Days(64);
        progressSummary.setImprovedKnowledgeCount(2);
        response.setProgressSummary(progressSummary);

        when(dashboardService.getOverview(nullable(AuthenticatedUser.class))).thenReturn(response);

        mockMvc.perform(get("/dashboard/overview").with(authentication(authenticationToken())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.weakKnowledgeItems[0].knowledgeId").value(1))
            .andExpect(jsonPath("$.data.recentTrainings[0].band.code").value("BASIC"))
            .andExpect(jsonPath("$.data.progressSummary.averageScoreLast7Days").value(64));
    }

    @Test
    void getReviewRemindersReturns200() throws Exception {
        ReviewReminderItemResponse reminder = new ReviewReminderItemResponse();
        reminder.setKnowledgeId(1L);
        reminder.setKnowledgeTitle("Spring");
        reminder.setReviewWeight(82);
        reminder.setReason("掌握度和最近得分都偏低，建议优先回练巩固关键点。");
        reminder.setSuggestedQuestionType("FUNDAMENTAL");
        reminder.setSuggestedDifficulty("EASY");
        reminder.setLastTrainedAt(LocalDateTime.of(2026, 3, 10, 9, 0));

        ReviewReminderResponse response = new ReviewReminderResponse();
        response.setItems(List.of(reminder));
        response.setGeneratedAt(LocalDateTime.of(2026, 3, 18, 10, 0));

        when(dashboardService.getReviewReminders(nullable(AuthenticatedUser.class))).thenReturn(response);

        mockMvc.perform(get("/dashboard/review-reminders").with(authentication(authenticationToken())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.items[0].knowledgeId").value(1))
            .andExpect(jsonPath("$.data.items[0].reviewWeight").value(82))
            .andExpect(jsonPath("$.data.items[0].suggestedQuestionType").value("FUNDAMENTAL"))
            .andExpect(jsonPath("$.data.items[0].suggestedDifficulty").value("EASY"));
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
