package com.philxin.interviewos.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.philxin.interviewos.common.GlobalExceptionHandler;
import com.philxin.interviewos.controller.dto.user.UserOnboardingResponse;
import com.philxin.interviewos.entity.AppUser;
import com.philxin.interviewos.entity.TargetRole;
import com.philxin.interviewos.security.AuthenticatedUser;
import com.philxin.interviewos.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = UserController.class, properties = "server.servlet.context-path=")
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @Test
    void updateOnboardingReturns200() throws Exception {
        AuthenticatedUser authenticatedUser = AuthenticatedUser.fromEntity(buildUserEntity());
        when(userService.updateOnboarding(nullable(AuthenticatedUser.class), eq(TargetRole.JAVA_BACKEND)))
            .thenReturn(buildResponse());

        String requestJson = objectMapper.writeValueAsString(new RequestBody("JAVA_BACKEND"));

        mockMvc.perform(
            patch("/users/me/onboarding")
                .with(authentication(new UsernamePasswordAuthenticationToken(authenticatedUser, null)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson)
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.id").value(1))
            .andExpect(jsonPath("$.data.targetRole").value("JAVA_BACKEND"));
    }

    @Test
    void updateOnboardingValidationFailureReturns400() throws Exception {
        String requestJson = objectMapper.writeValueAsString(new RequestBody(null));

        mockMvc.perform(
            patch("/users/me/onboarding")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson)
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(400));
    }

    private UserOnboardingResponse buildResponse() {
        UserOnboardingResponse response = new UserOnboardingResponse();
        response.setId(1L);
        response.setTargetRole("JAVA_BACKEND");
        return response;
    }

    private AppUser buildUserEntity() {
        AppUser user = new AppUser();
        user.setId(1L);
        user.setEmail("user@example.com");
        user.setDisplayName("philxin");
        user.setPasswordHash("hash");
        return user;
    }

    private record RequestBody(String targetRole) {
    }
}
