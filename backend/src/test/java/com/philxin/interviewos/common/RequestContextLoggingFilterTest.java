package com.philxin.interviewos.common;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.philxin.interviewos.config.SecurityConfig;
import com.philxin.interviewos.controller.KnowledgeController;
import com.philxin.interviewos.entity.AppUser;
import com.philxin.interviewos.entity.Knowledge;
import com.philxin.interviewos.entity.KnowledgeSourceType;
import com.philxin.interviewos.entity.KnowledgeStatus;
import com.philxin.interviewos.repository.AppUserRepository;
import com.philxin.interviewos.security.AuthenticatedUser;
import com.philxin.interviewos.security.JwtTokenService;
import com.philxin.interviewos.security.RestAccessDeniedHandler;
import com.philxin.interviewos.security.RestAuthenticationEntryPoint;
import com.philxin.interviewos.service.KnowledgeFileImportService;
import com.philxin.interviewos.service.KnowledgeImportService;
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
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = KnowledgeController.class,
    properties = {
        "server.servlet.context-path=",
        "app.security.jwt.secret=01234567890123456789012345678901"
    }
)
@AutoConfigureMockMvc
@Import({
    GlobalExceptionHandler.class,
    RequestContextLoggingFilter.class,
    SecurityConfig.class,
    RestAuthenticationEntryPoint.class,
    RestAccessDeniedHandler.class
})
class RequestContextLoggingFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KnowledgeService knowledgeService;

    @MockBean
    private KnowledgeImportService knowledgeImportService;

    @MockBean
    private KnowledgeFileImportService knowledgeFileImportService;

    @MockBean
    private JwtTokenService jwtTokenService;

    @MockBean
    private AppUserRepository appUserRepository;

    @Test
    void generatesRequestIdHeaderWhenClientDoesNotProvideOne() throws Exception {
        when(knowledgeService.getKnowledgeList(nullable(AuthenticatedUser.class))).thenReturn(List.of(buildKnowledge()));
        when(jwtTokenService.parseUserId("valid-token")).thenReturn(1L);
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(buildUser()));

        mockMvc.perform(get("/knowledge").header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
            .andExpect(status().isOk())
            .andExpect(header().string(RequestContextLoggingFilter.REQUEST_ID_HEADER, not(nullValue())));
    }

    @Test
    void preservesClientProvidedRequestId() throws Exception {
        when(knowledgeService.getKnowledgeList(nullable(AuthenticatedUser.class))).thenReturn(List.of(buildKnowledge()));
        when(jwtTokenService.parseUserId("valid-token")).thenReturn(1L);
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(buildUser()));

        mockMvc.perform(
            get("/knowledge")
                .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                .header(RequestContextLoggingFilter.REQUEST_ID_HEADER, "trace-knowledge-001")
        )
            .andExpect(status().isOk())
            .andExpect(header().string(RequestContextLoggingFilter.REQUEST_ID_HEADER, "trace-knowledge-001"));
    }

    private AppUser buildUser() {
        AppUser user = new AppUser();
        user.setId(1L);
        user.setEmail("user@example.com");
        user.setDisplayName("trace-user");
        user.setPasswordHash("hash");
        return user;
    }

    private Knowledge buildKnowledge() {
        Knowledge knowledge = new Knowledge();
        knowledge.setId(1L);
        knowledge.setTitle("Trace");
        knowledge.setContent("Request");
        knowledge.setMastery(0);
        knowledge.setSourceType(KnowledgeSourceType.MANUAL);
        knowledge.setStatus(KnowledgeStatus.ACTIVE);
        knowledge.setCreatedAt(LocalDateTime.of(2026, 3, 14, 0, 0, 0));
        knowledge.setUpdatedAt(LocalDateTime.of(2026, 3, 14, 0, 0, 0));
        return knowledge;
    }
}
