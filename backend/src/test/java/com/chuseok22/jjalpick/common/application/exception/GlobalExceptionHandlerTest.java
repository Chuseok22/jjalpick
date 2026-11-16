package com.chuseok22.jjalpick.common.application.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chuseok22.jjalpick.common.core.exception.CustomException;
import com.chuseok22.jjalpick.common.core.exception.ErrorCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ThrowingController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    void customException_returnsItsErrorCode() throws Exception {
        mockMvc.perform(get("/test/custom"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.errorCode").value("STATE_CONFLICT"))
            .andExpect(jsonPath("$.errorMessage").value(ErrorCode.STATE_CONFLICT.message()));
    }

    @Test
    void unexpectedException_returns500WithoutInternalDetails() throws Exception {
        String body = mockMvc.perform(get("/test/unexpected"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.errorCode").value("INTERNAL_SERVER_ERROR"))
            .andReturn().getResponse().getContentAsString();

        assertThat(body)
            .doesNotContain("jdbc")
            .doesNotContain("SELECT")
            .doesNotContain("IllegalStateException")
            .doesNotContain("at com.");
    }

    @Test
    void malformedJson_returns400MalformedRequestBody() throws Exception {
        mockMvc.perform(post("/test/body").contentType(MediaType.APPLICATION_JSON).content("{ \"broken\""))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("MALFORMED_REQUEST_BODY"));

        mockMvc.perform(post("/test/body").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("MALFORMED_REQUEST_BODY"));
    }

    @Test
    void invalidBody_returns400InvalidRequest() throws Exception {
        mockMvc.perform(post("/test/body").contentType(MediaType.APPLICATION_JSON).content("{ \"name\": \"\" }"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"))
            .andExpect(jsonPath("$.errorMessage").value("이름은 필수입니다"));
    }

    @Test
    void unsupportedMethod_returns405() throws Exception {
        mockMvc.perform(post("/test/custom"))
            .andExpect(status().isMethodNotAllowed())
            .andExpect(jsonPath("$.errorCode").value("METHOD_NOT_ALLOWED"));
    }

    record NameRequest(@NotBlank(message = "이름은 필수입니다") String name) {
    }

    @RestController
    static class ThrowingController {

        @GetMapping("/test/custom")
        void custom() {
            throw new CustomException(ErrorCode.STATE_CONFLICT);
        }

        @GetMapping("/test/unexpected")
        void unexpected() {
            throw new IllegalStateException("jdbc:postgresql://secret SELECT 1");
        }

        @PostMapping("/test/body")
        void body(@Valid @RequestBody NameRequest request) {
        }
    }
}
