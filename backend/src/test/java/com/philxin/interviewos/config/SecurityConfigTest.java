package com.philxin.interviewos.config;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.philxin.interviewos.common.GlobalExceptionHandler;
import com.philxin.interviewos.controller.KnowledgeController;
import com.philxin.interviewos.entity.Knowledge;
import com.philxin.interviewos.service.KnowledgeService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = KnowledgeController.class,
    properties = {
        "server.servlet.context-path=",
        "app.cors.allowed-origins[0]=http://localhost:5173",
        "app.cors.allowed-origins[1]=https://console.example.com"
    }
)
@AutoConfigureMockMvc
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KnowledgeService knowledgeService;

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
    void nonPublicEndpointIsDenied() throws Exception {
        mockMvc.perform(get("/internal/ping"))
            .andExpect(status().isForbidden());
    }

    @Test
    void publicEndpointIncludesBasicSecurityHeaders() throws Exception {
        when(knowledgeService.getKnowledgeList()).thenReturn(List.of(buildKnowledge()));

        mockMvc.perform(
            get("/knowledge")
                .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk())
            .andExpect(header().string("X-Content-Type-Options", "nosniff"))
            .andExpect(header().string("X-Frame-Options", "SAMEORIGIN"))
            .andExpect(header().string("Referrer-Policy", "no-referrer"));
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
