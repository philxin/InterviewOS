package com.philxin.interviewos.controller;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.philxin.interviewos.common.BusinessException;
import com.philxin.interviewos.common.GlobalExceptionHandler;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

    @Test
    void getKnowledgeListReturns200() throws Exception {
        when(knowledgeService.getKnowledgeList()).thenReturn(List.of(buildKnowledge(1L, "Spring Boot")));

        mockMvc.perform(get("/knowledge"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data[0].id").value(1))
            .andExpect(jsonPath("$.data[0].title").value("Spring Boot"));
    }

    @Test
    void getKnowledgeByIdNotFoundReturns404() throws Exception {
        doThrow(new BusinessException(HttpStatus.NOT_FOUND, "Knowledge not found with id: 99"))
            .when(knowledgeService)
            .getKnowledgeById(99L);

        mockMvc.perform(get("/knowledge/99"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value(404))
            .andExpect(jsonPath("$.message").value("Knowledge not found with id: 99"))
            .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void createKnowledgeReturns201() throws Exception {
        Knowledge knowledge = buildKnowledge(2L, "JPA");
        when(knowledgeService.createKnowledge("JPA", "ORM basics")).thenReturn(knowledge);

        String requestJson = objectMapper.writeValueAsString(new Request("JPA", "ORM basics"));
        mockMvc.perform(
            post("/knowledge")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson)
        )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.id").value(2))
            .andExpect(jsonPath("$.data.title").value("JPA"));
    }

    @Test
    void updateKnowledgeValidationFailureReturns400() throws Exception {
        String requestJson = objectMapper.writeValueAsString(new Request("", "content"));
        mockMvc.perform(
            put("/knowledge/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson)
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").exists());

        verify(knowledgeService, never()).updateKnowledge(anyLong(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void deleteKnowledgeReturns200() throws Exception {
        doNothing().when(knowledgeService).deleteKnowledge(1L);

        mockMvc.perform(delete("/knowledge/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.message").value("success"))
            .andExpect(jsonPath("$.data").value(nullValue()));
    }

    private Knowledge buildKnowledge(Long id, String title) {
        Knowledge knowledge = new Knowledge();
        knowledge.setId(id);
        knowledge.setTitle(title);
        knowledge.setContent("content");
        knowledge.setMastery(0);
        knowledge.setCreatedAt(LocalDateTime.of(2026, 3, 8, 0, 0, 0));
        return knowledge;
    }

    private record Request(String title, String content) {
    }
}
