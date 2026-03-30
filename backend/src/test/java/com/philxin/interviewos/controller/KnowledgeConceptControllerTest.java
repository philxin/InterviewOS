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
import com.philxin.interviewos.common.GlobalExceptionHandler;
import com.philxin.interviewos.controller.dto.knowledge.KnowledgeConceptResponse;
import com.philxin.interviewos.entity.AppUser;
import com.philxin.interviewos.security.AuthenticatedUser;
import com.philxin.interviewos.service.KnowledgeConceptService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = KnowledgeConceptController.class, properties = "server.servlet.context-path=")
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class KnowledgeConceptControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private KnowledgeConceptService knowledgeConceptService;

    @Test
    void listDocumentConceptsReturns200() throws Exception {
        UUID documentId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        KnowledgeConceptResponse response = buildResponse();
        when(knowledgeConceptService.listDocumentConcepts(nullable(AuthenticatedUser.class), eq(documentId)))
            .thenReturn(List.of(response));

        mockMvc.perform(get("/knowledge/documents/{documentId}/concepts", documentId).with(authentication(authenticationToken())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data[0].name").value("Redis Core"))
            .andExpect(jsonPath("$.data[0].status").value("CANDIDATE"));
    }

    @Test
    void acceptConceptReturns200() throws Exception {
        KnowledgeConceptResponse response = buildResponse();
        response.setStatus("ACCEPTED");
        response.setAcceptedKnowledgeId(88L);
        when(knowledgeConceptService.acceptConcept(nullable(AuthenticatedUser.class), eq(101L), org.mockito.ArgumentMatchers.any()))
            .thenReturn(response);

        String requestJson = objectMapper.writeValueAsString(new AcceptRequest("Redis 进阶", "内容摘要", List.of("redis")));
        mockMvc.perform(
            post("/knowledge/concepts/{conceptId}/accept", 101L)
                .with(authentication(authenticationToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson)
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.status").value("ACCEPTED"))
            .andExpect(jsonPath("$.data.acceptedKnowledgeId").value(88));
    }

    @Test
    void rejectConceptReturns200() throws Exception {
        KnowledgeConceptResponse response = buildResponse();
        response.setStatus("REJECTED");
        when(knowledgeConceptService.rejectConcept(nullable(AuthenticatedUser.class), eq(101L)))
            .thenReturn(response);

        mockMvc.perform(post("/knowledge/concepts/{conceptId}/reject", 101L).with(authentication(authenticationToken())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.status").value("REJECTED"));
    }

    private KnowledgeConceptResponse buildResponse() {
        KnowledgeConceptResponse response = new KnowledgeConceptResponse();
        response.setConceptId(101L);
        response.setDocumentId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
        response.setName("Redis Core");
        response.setSummary("Redis 核心机制摘要");
        response.setAliases(List.of("redis-core"));
        response.setSupportingChunkIds(List.of(11L, 12L));
        response.setConfidence(0.91d);
        response.setStatus("CANDIDATE");
        response.setCreatedAt(LocalDateTime.of(2026, 3, 30, 16, 0, 0));
        response.setUpdatedAt(LocalDateTime.of(2026, 3, 30, 16, 0, 0));
        return response;
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

    private record AcceptRequest(String title, String content, List<String> tags) {
    }
}
