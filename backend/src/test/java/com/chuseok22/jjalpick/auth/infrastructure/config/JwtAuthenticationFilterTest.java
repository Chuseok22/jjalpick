package com.chuseok22.jjalpick.auth.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.chuseok22.jjalpick.auth.core.port.TokenProvider;
import com.chuseok22.jjalpick.common.core.exception.CustomException;
import com.chuseok22.jjalpick.common.core.exception.ErrorCode;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import tools.jackson.databind.json.JsonMapper;

class JwtAuthenticationFilterTest {

    private static final UUID MEMBER_ID = UUID.fromString("0198a1b2-c3d4-7e5f-8a9b-0c1d2e3f4a5b");
    private static final String ACCESS_TOKEN = "access-token";

    private TokenProvider tokenProvider;
    private JwtAuthenticationFilter filter;
    private JwtAuthenticationEntryPoint entryPoint;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain filterChain;

    @BeforeEach
    void setUp() {
        tokenProvider = mock(TokenProvider.class);
        filter = new JwtAuthenticationFilter(tokenProvider);
        entryPoint = new JwtAuthenticationEntryPoint(JsonMapper.builder().build());
        request = new MockHttpServletRequest("GET", "/api/members/me");
        response = new MockHttpServletResponse();
        filterChain = new MockFilterChain();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilter_validBearerToken_setsAuthenticationWithMemberId() throws Exception {
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN);
        given(tokenProvider.parseAccessToken(ACCESS_TOKEN)).willReturn(MEMBER_ID);

        filter.doFilter(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isEqualTo(MEMBER_ID);
        assertThat(authentication.getAuthorities()).isEmpty();
        assertThat(filterChain.getRequest()).isSameAs(request);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Bearer", "bearer", "BEARER"})
    void doFilter_bearerSchemeCaseInsensitive_setsAuthentication(String scheme) throws Exception {
        request.addHeader(HttpHeaders.AUTHORIZATION, scheme + " " + ACCESS_TOKEN);
        given(tokenProvider.parseAccessToken(ACCESS_TOKEN)).willReturn(MEMBER_ID);

        filter.doFilter(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isEqualTo(MEMBER_ID);
    }

    @Test
    void doFilter_noAuthorizationHeader_continuesWithoutAuthentication() throws Exception {
        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(request.getAttribute(JwtAuthenticationFilter.ERROR_CODE_ATTRIBUTE)).isNull();
        assertThat(filterChain.getRequest()).isSameAs(request);
        verifyNoInteractions(tokenProvider);
    }

    @Test
    void doFilter_expiredToken_continuesWithoutAuthenticationAndMarksAccessTokenExpired() throws Exception {
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN);
        given(tokenProvider.parseAccessToken(ACCESS_TOKEN))
            .willThrow(new CustomException(ErrorCode.ACCESS_TOKEN_EXPIRED));

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(request.getAttribute(JwtAuthenticationFilter.ERROR_CODE_ATTRIBUTE))
            .isEqualTo(ErrorCode.ACCESS_TOKEN_EXPIRED);
        assertThat(filterChain.getRequest()).isSameAs(request);
    }

    @Test
    void doFilter_invalidToken_marksUnauthenticated() throws Exception {
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN);
        given(tokenProvider.parseAccessToken(ACCESS_TOKEN))
            .willThrow(new CustomException(ErrorCode.UNAUTHENTICATED));

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(request.getAttribute(JwtAuthenticationFilter.ERROR_CODE_ATTRIBUTE))
            .isEqualTo(ErrorCode.UNAUTHENTICATED);
        assertThat(filterChain.getRequest()).isSameAs(request);
    }

    @Test
    void entryPoint_expiredMarker_writes401AccessTokenExpiredBody() throws Exception {
        request.setAttribute(JwtAuthenticationFilter.ERROR_CODE_ATTRIBUTE, ErrorCode.ACCESS_TOKEN_EXPIRED);

        entryPoint.commence(request, response, new InsufficientAuthenticationException("인증 필요"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).startsWith("application/json");
        assertThat(response.getContentAsString())
            .contains("\"errorCode\":\"ACCESS_TOKEN_EXPIRED\"")
            .contains("\"errorMessage\":\"" + ErrorCode.ACCESS_TOKEN_EXPIRED.message() + "\"");
    }

    @Test
    void entryPoint_noMarker_writes401UnauthenticatedBody() throws Exception {
        entryPoint.commence(request, response, new InsufficientAuthenticationException("인증 필요"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString())
            .contains("\"errorCode\":\"UNAUTHENTICATED\"")
            .contains("\"errorMessage\":\"" + ErrorCode.UNAUTHENTICATED.message() + "\"");
    }

}
