package com.philxin.interviewos.common;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
            .standaloneSetup(new TestController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    void returnsBusinessExceptionWithMatchedStatusAndCode() throws Exception {
        mockMvc.perform(get("/test/business"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value(404))
            .andExpect(jsonPath("$.message").value("Knowledge not found"))
            .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void returnsBadRequestForMissingParameter() throws Exception {
        mockMvc.perform(get("/test/required"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value("Missing parameter: name"))
            .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void returnsInternalServerErrorForUnhandledException() throws Exception {
        mockMvc.perform(get("/test/system"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.code").value(500))
            .andExpect(jsonPath("$.message").value("Internal server error"))
            .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @RestController
    static class TestController {
        @GetMapping("/test/business")
        String business() {
            throw new BusinessException(HttpStatus.NOT_FOUND, "Knowledge not found");
        }

        @GetMapping("/test/system")
        String system() {
            throw new RuntimeException("Boom");
        }

        @GetMapping("/test/required")
        String required(@RequestParam("name") String name) {
            return name;
        }
    }
}
