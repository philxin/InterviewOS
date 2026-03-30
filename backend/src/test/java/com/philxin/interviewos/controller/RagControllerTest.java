package com.philxin.interviewos.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.philxin.interviewos.common.GlobalExceptionHandler;
import com.philxin.interviewos.entity.AppUser;
import com.philxin.interviewos.entity.KnowledgeChunk;
import com.philxin.interviewos.entity.KnowledgeChunkStatus;
import com.philxin.interviewos.entity.KnowledgeDocument;
import com.philxin.interviewos.entity.KnowledgeDocumentStatus;
import com.philxin.interviewos.security.AuthenticatedUser;
import com.philxin.interviewos.service.RetrievalService;
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

@WebMvcTest(controllers = RagController.class, properties = "server.servlet.context-path=")
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class RagControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RetrievalService retrievalService;

    @Test
    void searchReturns200() throws Exception {
        UUID documentId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        KnowledgeDocument document = new KnowledgeDocument();
        document.setId(documentId);
        document.setTitle("Java Notes");
        document.setStatus(KnowledgeDocumentStatus.ACTIVE);

        KnowledgeChunk chunk = new KnowledgeChunk();
        chunk.setId(1L);
        chunk.setDocument(document);
        chunk.setChunkIndex(0);
        chunk.setText("自动配置的核心是条件装配。");
        chunk.setStatus(KnowledgeChunkStatus.READY);
        chunk.setStartOffset(0);
        chunk.setEndOffset(16);

        RetrievalService.RetrievalMatch match = new RetrievalService.RetrievalMatch(chunk, 0.92d);
        RetrievalService.RetrievalResult result = new RetrievalService.RetrievalResult(
            "自动配置",
            null,
            5,
            false,
            List.of(match)
        );
        when(retrievalService.search(nullable(AuthenticatedUser.class), eq("自动配置"), eq(null), eq(null))).thenReturn(result);

        mockMvc.perform(get("/rag/search").with(authentication(authenticationToken())).param("query", "自动配置"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.hitCount").value(1))
            .andExpect(jsonPath("$.data.items[0].documentTitle").value("Java Notes"))
            .andExpect(jsonPath("$.data.items[0].score").value(0.92d));
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
