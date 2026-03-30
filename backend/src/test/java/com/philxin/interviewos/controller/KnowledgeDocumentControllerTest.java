package com.philxin.interviewos.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.philxin.interviewos.common.GlobalExceptionHandler;
import com.philxin.interviewos.controller.dto.knowledge.KnowledgeDocumentResponse;
import com.philxin.interviewos.entity.AppUser;
import com.philxin.interviewos.security.AuthenticatedUser;
import com.philxin.interviewos.service.KnowledgeDocumentService;
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

@WebMvcTest(controllers = KnowledgeDocumentController.class, properties = "server.servlet.context-path=")
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class KnowledgeDocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KnowledgeDocumentService knowledgeDocumentService;

    @Test
    void listDocumentsReturns200() throws Exception {
        KnowledgeDocumentResponse response = new KnowledgeDocumentResponse();
        response.setDocumentId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
        response.setImportId(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"));
        response.setTitle("Java Notes");
        response.setOriginalFileName("java-notes.pdf");
        response.setStatus("ACTIVE");
        response.setTotalChunks(12);
        response.setActiveChunks(12);
        response.setEmbeddingModel("text-embedding-3-large");
        response.setEmbeddingDim(3072);
        response.setUpdatedAt(LocalDateTime.of(2026, 3, 30, 10, 0));
        when(knowledgeDocumentService.listDocuments(nullable(AuthenticatedUser.class))).thenReturn(List.of(response));

        mockMvc.perform(get("/knowledge/documents").with(authentication(authenticationToken())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data[0].title").value("Java Notes"))
            .andExpect(jsonPath("$.data[0].embeddingModel").value("text-embedding-3-large"));
    }

    @Test
    void getDocumentReturns200() throws Exception {
        UUID documentId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        KnowledgeDocumentResponse response = new KnowledgeDocumentResponse();
        response.setDocumentId(documentId);
        response.setImportId(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"));
        response.setTitle("Java Notes");
        response.setOriginalFileName("java-notes.pdf");
        response.setStatus("ACTIVE");
        response.setTotalChunks(12);
        response.setActiveChunks(12);
        when(knowledgeDocumentService.getDocument(nullable(AuthenticatedUser.class), eq(documentId))).thenReturn(response);

        mockMvc.perform(get("/knowledge/documents/{documentId}", documentId).with(authentication(authenticationToken())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.documentId").value(documentId.toString()))
            .andExpect(jsonPath("$.data.totalChunks").value(12));
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
