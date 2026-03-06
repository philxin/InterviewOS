package com.philxin.interviewos.common;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.nullValue;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = GlobalExceptionHandlerTest.TestController.class)
@Import(GlobalExceptionHandler.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsBusinessExceptionWithMatchedStatusAndCode() throws Exception {
        mockMvc.perform(get("/test/business"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value(404))
            .andExpect(jsonPath("$.message").value("Knowledge not found"))
            .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void returnsBadRequestForValidationFailure() throws Exception {
        mockMvc.perform(
            post("/test/valid")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"\"}")
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").exists());
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

        @PostMapping("/test/valid")
        String valid(@Valid @RequestBody ValidationRequest request) {
            return request.title();
        }
    }

    record ValidationRequest(@NotBlank String title) {
    }
}
