package com.philxin.interviewos.common;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.philxin.interviewos.config.SecurityConfig;
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
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = KnowledgeController.class, properties = "server.servlet.context-path=")
@AutoConfigureMockMvc
@Import({GlobalExceptionHandler.class, RequestContextLoggingFilter.class, SecurityConfig.class})
class RequestContextLoggingFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KnowledgeService knowledgeService;

    @Test
    void generatesRequestIdHeaderWhenClientDoesNotProvideOne() throws Exception {
        when(knowledgeService.getKnowledgeList()).thenReturn(List.of(buildKnowledge()));

        mockMvc.perform(get("/knowledge"))
            .andExpect(status().isOk())
            .andExpect(header().string(RequestContextLoggingFilter.REQUEST_ID_HEADER, not(nullValue())));
    }

    @Test
    void preservesClientProvidedRequestId() throws Exception {
        when(knowledgeService.getKnowledgeList()).thenReturn(List.of(buildKnowledge()));

        mockMvc.perform(
            get("/knowledge")
                .header(RequestContextLoggingFilter.REQUEST_ID_HEADER, "trace-knowledge-001")
        )
            .andExpect(status().isOk())
            .andExpect(header().string(RequestContextLoggingFilter.REQUEST_ID_HEADER, "trace-knowledge-001"));
    }

    private Knowledge buildKnowledge() {
        Knowledge knowledge = new Knowledge();
        knowledge.setId(1L);
        knowledge.setTitle("Trace");
        knowledge.setContent("Request");
        knowledge.setMastery(0);
        knowledge.setCreatedAt(LocalDateTime.of(2026, 3, 14, 0, 0, 0));
        return knowledge;
    }
}
