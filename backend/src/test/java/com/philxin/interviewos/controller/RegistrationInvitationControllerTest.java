package com.philxin.interviewos.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.philxin.interviewos.common.GlobalExceptionHandler;
import com.philxin.interviewos.controller.dto.invitation.RegistrationInvitationResponse;
import com.philxin.interviewos.entity.AppUser;
import com.philxin.interviewos.security.AuthenticatedUser;
import com.philxin.interviewos.service.RegistrationInvitationService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = RegistrationInvitationController.class, properties = "server.servlet.context-path=")
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class RegistrationInvitationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RegistrationInvitationService registrationInvitationService;

    @Test
    void createInvitationReturns201() throws Exception {
        AuthenticatedUser authenticatedUser = AuthenticatedUser.fromEntity(buildUserEntity());
        RegistrationInvitationResponse response = new RegistrationInvitationResponse();
        response.setInvitationCode("11111111-1111-1111-1111-111111111111");
        response.setInviteeEmail("invitee@example.com");
        response.setRegistrationPath("/invite/11111111-1111-1111-1111-111111111111");
        response.setExpiresAt(LocalDateTime.of(2026, 3, 31, 10, 0));
        when(registrationInvitationService.createInvitation(nullable(AuthenticatedUser.class), eq("invitee@example.com")))
            .thenReturn(response);

        mockMvc.perform(
            post("/invitations")
                .with(authentication(new UsernamePasswordAuthenticationToken(authenticatedUser, null)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RequestBody("invitee@example.com")))
        )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.registrationPath").value("/invite/11111111-1111-1111-1111-111111111111"));
    }

    @Test
    void createInvitationValidationFailureReturns400() throws Exception {
        mockMvc.perform(
            post("/invitations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RequestBody("")))
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(400));
    }

    private AppUser buildUserEntity() {
        AppUser user = new AppUser();
        user.setId(1L);
        user.setEmail("user@example.com");
        user.setDisplayName("philxin");
        user.setPasswordHash("hash");
        return user;
    }

    private record RequestBody(String email) {
    }
}
