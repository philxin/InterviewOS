package com.philxin.interviewos.controller;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.philxin.interviewos.common.BusinessException;
import com.philxin.interviewos.common.GlobalExceptionHandler;
import com.philxin.interviewos.controller.dto.knowledge.BatchImportKnowledgeResponse;
import com.philxin.interviewos.entity.AppUser;
import com.philxin.interviewos.entity.Knowledge;
import com.philxin.interviewos.entity.KnowledgeSourceType;
import com.philxin.interviewos.entity.KnowledgeStatus;
import com.philxin.interviewos.entity.KnowledgeTag;
import com.philxin.interviewos.security.AuthenticatedUser;
import com.philxin.interviewos.service.KnowledgeImportService;
import com.philxin.interviewos.service.KnowledgeService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = KnowledgeController.class, properties = "server.servlet.context-path=")
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class KnowledgeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private KnowledgeService knowledgeService;

    @MockBean
    private KnowledgeImportService knowledgeImportService;

    @Test
    void getKnowledgeListReturns200() throws Exception {
        when(knowledgeService.getKnowledgeList(nullable(AuthenticatedUser.class)))
            .thenReturn(List.of(buildKnowledge(1L, "Spring Boot", List.of("spring", "backend"))));

        mockMvc.perform(get("/knowledge").with(authentication(authenticationToken())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data[0].id").value(1))
            .andExpect(jsonPath("$.data[0].title").value("Spring Boot"))
            .andExpect(jsonPath("$.data[0].tags[0]").value("spring"))
            .andExpect(jsonPath("$.data[0].sourceType").value("MANUAL"))
            .andExpect(jsonPath("$.data[0].status").value("ACTIVE"));
    }

    @Test
    void getKnowledgeByIdNotFoundReturns404() throws Exception {
        doThrow(new BusinessException(HttpStatus.NOT_FOUND, "Knowledge not found with id: 99"))
            .when(knowledgeService)
            .getKnowledgeById(nullable(AuthenticatedUser.class), eq(99L));

        mockMvc.perform(get("/knowledge/99").with(authentication(authenticationToken())))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value(404))
            .andExpect(jsonPath("$.message").value("Knowledge not found with id: 99"))
            .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void createKnowledgeReturns201() throws Exception {
        Knowledge knowledge = buildKnowledge(2L, "JPA", List.of("spring", "backend"));
        when(
            knowledgeService.createKnowledge(
                nullable(AuthenticatedUser.class),
                eq("JPA"),
                eq("ORM basics"),
                eq(List.of(" Spring ", "Backend"))
            )
        ).thenReturn(knowledge);

        String requestJson = objectMapper.writeValueAsString(new Request("JPA", "ORM basics", List.of(" Spring ", "Backend")));
        mockMvc.perform(
            post("/knowledge")
                .with(authentication(authenticationToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson)
        )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.id").value(2))
            .andExpect(jsonPath("$.data.title").value("JPA"))
            .andExpect(jsonPath("$.data.tags[1]").value("backend"));
    }

    @Test
    void getKnowledgeTagsReturns200() throws Exception {
        when(knowledgeService.getKnowledgeTags(nullable(AuthenticatedUser.class)))
            .thenReturn(List.of("backend", "spring"));

        mockMvc.perform(get("/knowledge/tags").with(authentication(authenticationToken())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data[0]").value("backend"))
            .andExpect(jsonPath("$.data[1]").value("spring"));
    }

    @Test
    void batchImportReturns201WhenAtLeastOneItemCreated() throws Exception {
        BatchImportKnowledgeResponse response = new BatchImportKnowledgeResponse();
        response.setCreatedCount(1);
        response.setFailedCount(1);
        BatchImportKnowledgeResponse.FailedItem failedItem = new BatchImportKnowledgeResponse.FailedItem();
        failedItem.setIndex(1);
        failedItem.setTitle("Invalid");
        failedItem.setReason("content: content must not be blank");
        response.setFailedItems(List.of(failedItem));
        when(knowledgeImportService.batchImportKnowledge(nullable(AuthenticatedUser.class), org.mockito.ArgumentMatchers.anyList()))
            .thenReturn(response);

        String requestJson = objectMapper.writeValueAsString(
            new BatchImportRequest(List.of(
                new Request("Spring", "content", List.of("spring")),
                new Request("Invalid", "", List.of("redis"))
            ))
        );

        mockMvc.perform(
            post("/knowledge/batch-import")
                .with(authentication(authenticationToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson)
        )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.createdCount").value(1))
            .andExpect(jsonPath("$.data.failedCount").value(1))
            .andExpect(jsonPath("$.data.failedItems[0].index").value(1));
    }

    @Test
    void batchImportReturns422WhenAllItemsFail() throws Exception {
        BatchImportKnowledgeResponse response = new BatchImportKnowledgeResponse();
        response.setCreatedCount(0);
        response.setFailedCount(1);
        BatchImportKnowledgeResponse.FailedItem failedItem = new BatchImportKnowledgeResponse.FailedItem();
        failedItem.setIndex(0);
        failedItem.setTitle("Invalid");
        failedItem.setReason("content: content must not be blank");
        response.setFailedItems(List.of(failedItem));
        when(knowledgeImportService.batchImportKnowledge(nullable(AuthenticatedUser.class), org.mockito.ArgumentMatchers.anyList()))
            .thenReturn(response);

        String requestJson = objectMapper.writeValueAsString(
            new BatchImportRequest(List.of(new Request("Invalid", "", List.of("redis"))))
        );

        mockMvc.perform(
            post("/knowledge/batch-import")
                .with(authentication(authenticationToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson)
        )
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value(422))
            .andExpect(jsonPath("$.message").value("Batch import validation failed"))
            .andExpect(jsonPath("$.data.failedItems[0].reason").value("content: content must not be blank"));
    }

    @Test
    void updateKnowledgeValidationFailureReturns400() throws Exception {
        String requestJson = objectMapper.writeValueAsString(new Request("", "content", List.of("spring")));
        mockMvc.perform(
            put("/knowledge/1")
                .with(authentication(authenticationToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson)
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").exists());

        verify(knowledgeService, never()).updateKnowledge(
            nullable(AuthenticatedUser.class),
            anyLong(),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyList()
        );
    }

    @Test
    void deleteKnowledgeReturns200() throws Exception {
        doNothing().when(knowledgeService).deleteKnowledge(nullable(AuthenticatedUser.class), eq(1L));

        mockMvc.perform(delete("/knowledge/1").with(authentication(authenticationToken())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.message").value("success"))
            .andExpect(jsonPath("$.data").value(nullValue()));
    }

    private UsernamePasswordAuthenticationToken authenticationToken() {
        return new UsernamePasswordAuthenticationToken(
            AuthenticatedUser.fromEntity(buildUserEntity()),
            null
        );
    }

    private AppUser buildUserEntity() {
        AppUser user = new AppUser();
        user.setId(1L);
        user.setEmail("user@example.com");
        user.setDisplayName("philxin");
        user.setPasswordHash("hash");
        return user;
    }

    private Knowledge buildKnowledge(Long id, String title, List<String> tags) {
        Knowledge knowledge = new Knowledge();
        knowledge.setId(id);
        knowledge.setUser(buildUserEntity());
        knowledge.setTitle(title);
        knowledge.setContent("content");
        knowledge.setMastery(0);
        knowledge.setSourceType(KnowledgeSourceType.MANUAL);
        knowledge.setStatus(KnowledgeStatus.ACTIVE);
        knowledge.setCreatedAt(LocalDateTime.of(2026, 3, 8, 0, 0, 0));
        knowledge.setUpdatedAt(LocalDateTime.of(2026, 3, 8, 0, 0, 0));
        for (String tag : tags) {
            KnowledgeTag knowledgeTag = new KnowledgeTag();
            knowledgeTag.setKnowledge(knowledge);
            knowledgeTag.setTag(tag);
            knowledge.getTags().add(knowledgeTag);
        }
        return knowledge;
    }

    private record Request(String title, String content, List<String> tags) {
    }

    private record BatchImportRequest(List<Request> items) {
    }
}
