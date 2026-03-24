package com.philxin.interviewos.controller;

import static org.mockito.Mockito.when;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.nullable;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.philxin.interviewos.common.GlobalExceptionHandler;
import com.philxin.interviewos.controller.dto.auth.AuthResponse;
import com.philxin.interviewos.controller.dto.auth.AuthUserResponse;
import com.philxin.interviewos.controller.dto.invitation.PublicRegistrationInvitationResponse;
import com.philxin.interviewos.security.AuthenticatedUser;
import com.philxin.interviewos.service.AuthService;
import com.philxin.interviewos.service.RegistrationInvitationService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AuthController.class, properties = "server.servlet.context-path=")
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private RegistrationInvitationService registrationInvitationService;

    @Test
    void registerReturns201() throws Exception {
        when(authService.register("invite-code", "user@example.com", "Password123!", "philxin"))
            .thenReturn(buildAuthResponse());

        String requestJson = objectMapper.writeValueAsString(
            new RegisterBody("invite-code", "user@example.com", "Password123!", "philxin")
        );

        mockMvc.perform(
            post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson)
        )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.data.user.email").value("user@example.com"));
    }

    @Test
    void loginReturns200() throws Exception {
        when(authService.login("user@example.com", "Password123!")).thenReturn(buildAuthResponse());

        String requestJson = objectMapper.writeValueAsString(new LoginBody("user@example.com", "Password123!"));

        mockMvc.perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson)
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.expiresIn").value(604800));
    }

    @Test
    void meReturnsCurrentUser() throws Exception {
        AuthenticatedUser authenticatedUser = AuthenticatedUser.fromEntity(buildUserEntity());
        when(authService.getCurrentUser(nullable(AuthenticatedUser.class))).thenReturn(buildUserResponse());

        mockMvc.perform(
            get("/auth/me").with(authentication(new UsernamePasswordAuthenticationToken(authenticatedUser, null)))
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.displayName").value("philxin"))
            .andExpect(jsonPath("$.data.targetRole").value(nullValue()));
    }

    @Test
    void registerValidationFailureReturns400() throws Exception {
        String requestJson = objectMapper.writeValueAsString(new RegisterBody("", "", "weak", ""));

        mockMvc.perform(
            post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson)
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void getInvitationReturns200() throws Exception {
        PublicRegistrationInvitationResponse response = new PublicRegistrationInvitationResponse();
        response.setInvitationCode("11111111-1111-1111-1111-111111111111");
        response.setInviteeEmail("user@example.com");
        response.setExpiresAt(LocalDateTime.of(2026, 3, 31, 10, 0));
        when(registrationInvitationService.getInvitation("11111111-1111-1111-1111-111111111111"))
            .thenReturn(response);

        mockMvc.perform(get("/auth/invitations/11111111-1111-1111-1111-111111111111"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.inviteeEmail").value("user@example.com"));
    }

    private AuthResponse buildAuthResponse() {
        AuthResponse response = new AuthResponse();
        response.setToken("jwt-token");
        response.setTokenType("Bearer");
        response.setExpiresIn(604800);
        response.setUser(buildUserResponse());
        return response;
    }

    private AuthUserResponse buildUserResponse() {
        AuthUserResponse response = new AuthUserResponse();
        response.setId(1L);
        response.setEmail("user@example.com");
        response.setDisplayName("philxin");
        return response;
    }

    private com.philxin.interviewos.entity.AppUser buildUserEntity() {
        com.philxin.interviewos.entity.AppUser user = new com.philxin.interviewos.entity.AppUser();
        user.setId(1L);
        user.setEmail("user@example.com");
        user.setDisplayName("philxin");
        user.setPasswordHash("hash");
        return user;
    }

    private record RegisterBody(String invitationCode, String email, String password, String displayName) {
    }

    private record LoginBody(String email, String password) {
    }
}
