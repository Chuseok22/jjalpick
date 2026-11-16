package com.chuseok22.jjalpick.auth.application.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chuseok22.jjalpick.auth.application.dto.request.LoginRequest;
import com.chuseok22.jjalpick.auth.application.service.AuthService;
import com.chuseok22.jjalpick.auth.application.service.AuthTokens;
import com.chuseok22.jjalpick.auth.core.constant.ClientType;
import com.chuseok22.jjalpick.common.application.exception.GlobalExceptionHandler;
import jakarta.servlet.http.Cookie;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AuthControllerTest {

    private static final Instant NOW = Instant.parse("2026-11-15T00:00:00Z");
    private static final Instant ACCESS_EXPIRES_AT = NOW.plus(Duration.ofHours(1));
    private static final Instant REFRESH_EXPIRES_AT = NOW.plus(Duration.ofDays(7));
    private static final String ACCESS_TOKEN = "access-token";
    private static final String REFRESH_TOKEN = "refresh-token";
    private static final String COOKIE_REFRESH_TOKEN = "cookie-refresh-token";

    private AuthService authService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService, clock))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    private AuthTokens tokens(ClientType clientType) {
        return new AuthTokens(ACCESS_TOKEN, ACCESS_EXPIRES_AT, REFRESH_TOKEN, REFRESH_EXPIRES_AT, clientType);
    }

    private String loginBody(String idToken, ClientType clientType) {
        return """
            {"idToken":"%s","clientType":"%s","deviceIdentifier":"device-1","deviceName":"내 PC"}
            """.formatted(idToken, clientType.name());
    }

    @Test
    void login_desktop_returnsRefreshTokenInBodyAndNoCookie() throws Exception {
        given(authService.login(any(LoginRequest.class))).willReturn(tokens(ClientType.DESKTOP));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(loginBody("id-token", ClientType.DESKTOP)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").value(ACCESS_TOKEN))
            .andExpect(jsonPath("$.accessTokenExpiresAt").exists())
            .andExpect(jsonPath("$.refreshToken").value(REFRESH_TOKEN))
            .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
    }

    @Test
    void login_website_omitsRefreshTokenFromBodyAndSetsHttpOnlyStrictCookie() throws Exception {
        given(authService.login(any(LoginRequest.class))).willReturn(tokens(ClientType.WEBSITE));

        MvcResult result = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(loginBody("id-token", ClientType.WEBSITE)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").value(ACCESS_TOKEN))
            .andExpect(jsonPath("$.refreshToken").doesNotExist())
            .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).doesNotContain("refreshToken");
        String setCookie = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookie)
            .startsWith("refreshToken=" + REFRESH_TOKEN)
            .contains("Path=/api/auth")
            .contains("HttpOnly")
            .contains("Secure")
            .contains("SameSite=Strict")
            .contains("Max-Age=" + Duration.ofDays(7).toSeconds())
            .doesNotContain("Domain=");
    }

    @Test
    void login_blankIdToken_returns400InvalidRequest() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(loginBody(" ", ClientType.DESKTOP)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"));

        verifyNoInteractions(authService);
    }

    @Test
    void refresh_websiteCookieOnly_usesCookieValue() throws Exception {
        given(authService.refresh(COOKIE_REFRESH_TOKEN, "device-1")).willReturn(tokens(ClientType.WEBSITE));

        mockMvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .cookie(new Cookie("refreshToken", COOKIE_REFRESH_TOKEN))
                .content("{\"deviceIdentifier\":\"device-1\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").value(ACCESS_TOKEN))
            .andExpect(jsonPath("$.refreshToken").doesNotExist())
            .andExpect(header().string(HttpHeaders.SET_COOKIE, startsWith("refreshToken=" + REFRESH_TOKEN)));

        verify(authService).refresh(COOKIE_REFRESH_TOKEN, "device-1");
    }

    @Test
    void logout_withoutAnyToken_returns204() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
            .andExpect(status().isNoContent())
            .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));

        verify(authService).logout(null);
    }

    @Test
    void logout_cookiePresent_returns204AndExpiresCookie() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/logout")
                .cookie(new Cookie("refreshToken", COOKIE_REFRESH_TOKEN)))
            .andExpect(status().isNoContent())
            .andReturn();

        verify(authService).logout(COOKIE_REFRESH_TOKEN);
        assertThat(result.getResponse().getHeader(HttpHeaders.SET_COOKIE))
            .startsWith("refreshToken=;")
            .contains("Max-Age=0")
            .contains("Path=/api/auth")
            .contains("HttpOnly")
            .contains("Secure")
            .contains("SameSite=Strict");
    }

}
