package com.philxin.interviewos.config;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.philxin.interviewos.common.GlobalExceptionHandler;
import com.philxin.interviewos.controller.AuthController;
import com.philxin.interviewos.controller.KnowledgeController;
import com.philxin.interviewos.entity.AppUser;
import com.philxin.interviewos.entity.Knowledge;
import com.philxin.interviewos.service.AuthService;
import com.philxin.interviewos.service.KnowledgeService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import com.philxin.interviewos.repository.AppUserRepository;
import com.philxin.interviewos.security.JwtTokenService;
import com.philxin.interviewos.security.RestAccessDeniedHandler;
import com.philxin.interviewos.security.RestAuthenticationEntryPoint;

@WebMvcTest(
    controllers = {KnowledgeController.class, AuthController.class},
    properties = {
        "server.servlet.context-path=",
        "app.cors.allowed-origins[0]=http://localhost:5173",
        "app.cors.allowed-origins[1]=https://console.example.com",
        "app.security.jwt.secret=01234567890123456789012345678901"
    }
)
@AutoConfigureMockMvc
@Import({
    GlobalExceptionHandler.class,
    SecurityConfig.class,
    RestAuthenticationEntryPoint.class,
    RestAccessDeniedHandler.class
})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KnowledgeService knowledgeService;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtTokenService jwtTokenService;

    @MockBean
    private AppUserRepository appUserRepository;

    @Test
    void preflightRequestFromAllowedOriginReturnsCorsHeaders() throws Exception {
        mockMvc.perform(
            options("/knowledge")
                .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization,Content-Type")
        )
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"))
            .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"))
            .andExpect(header().string(HttpHeaders.VARY, org.hamcrest.Matchers.containsString("Origin")));
    }

    @Test
    void preflightRequestFromUnknownOriginIsRejected() throws Exception {
        mockMvc.perform(
            options("/knowledge")
                .header(HttpHeaders.ORIGIN, "https://unknown.example.com")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
        )
            .andExpect(status().isForbidden());
    }

    @Test
    void protectedEndpointWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/knowledge"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value(401))
            .andExpect(jsonPath("$.message").value("Unauthorized"));
    }

    @Test
    void publicAuthEndpointRemainsAccessibleWithoutToken() throws Exception {
        mockMvc.perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void authenticatedEndpointIncludesBasicSecurityHeaders() throws Exception {
        when(jwtTokenService.parseUserId("valid-token")).thenReturn(1L);
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(buildUser()));
        when(knowledgeService.getKnowledgeList()).thenReturn(List.of(buildKnowledge()));

        mockMvc.perform(
            get("/knowledge")
                .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk())
            .andExpect(header().string("X-Content-Type-Options", "nosniff"))
            .andExpect(header().string("X-Frame-Options", "SAMEORIGIN"))
            .andExpect(header().string("Referrer-Policy", "no-referrer"));
    }

    private AppUser buildUser() {
        AppUser user = new AppUser();
        user.setId(1L);
        user.setEmail("user@example.com");
        user.setDisplayName("philxin");
        user.setPasswordHash("hash");
        return user;
    }

    private Knowledge buildKnowledge() {
        Knowledge knowledge = new Knowledge();
        knowledge.setId(1L);
        knowledge.setTitle("Security");
        knowledge.setContent("cors");
        knowledge.setMastery(0);
        knowledge.setCreatedAt(LocalDateTime.of(2026, 3, 14, 0, 0, 0));
        return knowledge;
    }
}
