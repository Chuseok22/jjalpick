package com.chuseok22.mimefit.common.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chuseok22.mimefit.common.application.exception.GlobalExceptionHandler;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.json.JsonMapper;

class KoreaTimeJacksonConfigTest {

    private static final Instant EXPECTED_INSTANT = Instant.parse("2026-11-09T05:00:00Z");

    private JsonMapper jsonMapper;

    @BeforeEach
    void setUp() {
        JsonMapper.Builder builder = JsonMapper.builder();
        new KoreaTimeJacksonConfig().koreaTimeJsonMapperBuilderCustomizer().customize(builder);
        jsonMapper = builder.build();
    }

    @Test
    void serialize_instantAsKstOffset() {
        String json = jsonMapper.writeValueAsString(EXPECTED_INSTANT);

        assertThat(json).isEqualTo("\"2026-11-09T14:00:00+09:00\"");
    }

    @Test
    void deserialize_offsetValues_resolveToSameInstant() {
        assertThat(jsonMapper.readValue("\"2026-11-09T05:00:00Z\"", Instant.class)).isEqualTo(EXPECTED_INSTANT);
        assertThat(jsonMapper.readValue("\"2026-11-09T14:00:00+09:00\"", Instant.class)).isEqualTo(EXPECTED_INSTANT);
        assertThat(jsonMapper.readValue("\"2026-11-09T00:00:00-05:00\"", Instant.class)).isEqualTo(EXPECTED_INSTANT);
    }

    @Test
    void deserialize_withoutOffset_throws() {
        assertThatThrownBy(() -> jsonMapper.readValue("\"2026-11-09T14:00:00\"", Instant.class))
            .isInstanceOf(DatabindException.class);
    }

    @Test
    void deserialize_epochNumber_throws() {
        assertThatThrownBy(() -> jsonMapper.readValue("1762664400", Instant.class))
            .isInstanceOf(DatabindException.class);
        assertThatThrownBy(() -> jsonMapper.readValue("\"1762664400\"", Instant.class))
            .isInstanceOf(DatabindException.class);
    }

    @Test
    void roundTrip_keepsInstant() {
        String json = jsonMapper.writeValueAsString(EXPECTED_INSTANT);

        assertThat(jsonMapper.readValue(json, Instant.class)).isEqualTo(EXPECTED_INSTANT);
    }

    @Test
    void requestWithoutOffset_returns400MalformedRequestBody() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new InstantController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .setMessageConverters(new JacksonJsonHttpMessageConverter(jsonMapper))
            .build();

        mockMvc.perform(post("/test/instant").contentType(MediaType.APPLICATION_JSON)
                .content("{\"at\":\"2026-11-09T14:00:00\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("MALFORMED_REQUEST_BODY"));

        mockMvc.perform(post("/test/instant").contentType(MediaType.APPLICATION_JSON)
                .content("{\"at\":\"2026-11-09T14:00:00+09:00\"}"))
            .andExpect(status().isOk());
    }

    @Test
    void requestWithEpochValue_returns400MalformedRequestBody() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new InstantController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .setMessageConverters(new JacksonJsonHttpMessageConverter(jsonMapper))
            .build();

        mockMvc.perform(post("/test/instant").contentType(MediaType.APPLICATION_JSON)
                .content("{\"at\":1762664400}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("MALFORMED_REQUEST_BODY"));

        mockMvc.perform(post("/test/instant").contentType(MediaType.APPLICATION_JSON)
                .content("{\"at\":\"1762664400\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("MALFORMED_REQUEST_BODY"));
    }

    record InstantRequest(Instant at) {

    }

    @RestController
    static class InstantController {

        @PostMapping("/test/instant")
        void receive(@RequestBody InstantRequest request) {
        }

    }

}
