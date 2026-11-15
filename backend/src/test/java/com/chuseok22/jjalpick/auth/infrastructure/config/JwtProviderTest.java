package com.chuseok22.jjalpick.auth.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.chuseok22.jjalpick.auth.core.port.IssuedToken;
import com.chuseok22.jjalpick.auth.infrastructure.properties.JwtProperties;
import com.chuseok22.jjalpick.common.core.exception.CustomException;
import com.chuseok22.jjalpick.common.core.exception.ErrorCode;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtProviderTest {

    private static final Instant NOW = Instant.parse("2026-11-15T00:00:00Z");
    private static final long ACCESS_EXP_MILLIS = Duration.ofMinutes(30).toMillis();
    private static final long REFRESH_EXP_MILLIS = Duration.ofDays(30).toMillis();
    private static final String ISSUER = "jjalpick-test";
    private static final UUID MEMBER_ID = UUID.fromString("0198a1b2-c3d4-7e5f-8a9b-0c1d2e3f4a5b");
    private static final String SECRET_KEY = generateSecretKey(64);

    private static String generateSecretKey(int byteLength) {
        byte[] keyBytes = new byte[byteLength];
        new SecureRandom().nextBytes(keyBytes);
        return Base64.getEncoder().encodeToString(keyBytes);
    }

    private JwtProvider providerAt(Instant instant) {
        JwtProperties properties = new JwtProperties(SECRET_KEY, ACCESS_EXP_MILLIS, REFRESH_EXP_MILLIS, ISSUER);
        return new JwtProvider(properties, Clock.fixed(instant, ZoneOffset.UTC));
    }

    private void assertFailsWith(Runnable action, ErrorCode expected) {
        assertThatThrownBy(action::run)
            .isInstanceOfSatisfying(CustomException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo(expected));
    }

    @Test
    void issueAccessToken_thenParse_returnsMemberId() {
        JwtProvider provider = providerAt(NOW);

        IssuedToken issued = provider.issueAccessToken(MEMBER_ID);

        assertThat(provider.parseAccessToken(issued.token())).isEqualTo(MEMBER_ID);
    }

    @Test
    void issueAccessToken_expiresAtIsNowPlusAccessExpMillis() {
        IssuedToken issued = providerAt(NOW).issueAccessToken(MEMBER_ID);

        assertThat(issued.expiresAt()).isEqualTo(NOW.plusMillis(ACCESS_EXP_MILLIS));
    }

    @Test
    void parseAccessToken_afterExpiry_throwsAccessTokenExpired() {
        String token = providerAt(NOW).issueAccessToken(MEMBER_ID).token();
        JwtProvider later = providerAt(NOW.plusMillis(ACCESS_EXP_MILLIS + 1_000));

        assertFailsWith(() -> later.parseAccessToken(token), ErrorCode.ACCESS_TOKEN_EXPIRED);
    }

    @Test
    void parseAccessToken_tamperedSignature_throwsUnauthenticated() {
        JwtProvider provider = providerAt(NOW);
        String token = provider.issueAccessToken(MEMBER_ID).token();
        // 마지막 문자는 Base64 패딩 비트만 바뀔 수 있어 서명 중간 문자를 바꾼다
        int tamperIndex = token.lastIndexOf('.') + 10;
        char replacement = token.charAt(tamperIndex) == 'A' ? 'B' : 'A';
        String tampered = token.substring(0, tamperIndex) + replacement + token.substring(tamperIndex + 1);

        assertFailsWith(() -> provider.parseAccessToken(tampered), ErrorCode.UNAUTHENTICATED);
    }

    @Test
    void parseAccessToken_garbage_throwsUnauthenticated() {
        JwtProvider provider = providerAt(NOW);

        assertFailsWith(() -> provider.parseAccessToken("not-a-jwt"), ErrorCode.UNAUTHENTICATED);
    }

    @Test
    void parseAccessToken_refreshTokenGiven_throwsUnauthenticated() {
        JwtProvider provider = providerAt(NOW);
        String refreshToken = provider.issueRefreshToken(MEMBER_ID).token();

        assertFailsWith(() -> provider.parseAccessToken(refreshToken), ErrorCode.UNAUTHENTICATED);
    }

    @Test
    void parseRefreshToken_accessTokenGiven_throwsInvalidRefreshToken() {
        JwtProvider provider = providerAt(NOW);
        String accessToken = provider.issueAccessToken(MEMBER_ID).token();

        assertFailsWith(() -> provider.parseRefreshToken(accessToken), ErrorCode.INVALID_REFRESH_TOKEN);
    }

    @Test
    void parseRefreshToken_afterExpiry_throwsInvalidRefreshToken() {
        String token = providerAt(NOW).issueRefreshToken(MEMBER_ID).token();
        JwtProvider later = providerAt(NOW.plusMillis(REFRESH_EXP_MILLIS + 1_000));

        assertFailsWith(() -> later.parseRefreshToken(token), ErrorCode.INVALID_REFRESH_TOKEN);
    }

    @Test
    void issueRefreshToken_twiceAtSameInstant_returnsDifferentTokens() {
        JwtProvider provider = providerAt(NOW);

        String first = provider.issueRefreshToken(MEMBER_ID).token();
        String second = provider.issueRefreshToken(MEMBER_ID).token();

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void issueAccessToken_clockWithMillis_expiresAtTruncatedToSeconds() {
        Instant withMillis = NOW.plusMillis(789);

        IssuedToken issued = providerAt(withMillis).issueAccessToken(MEMBER_ID);

        assertThat(issued.expiresAt()).isEqualTo(NOW.plusMillis(ACCESS_EXP_MILLIS));
    }

    @Test
    void toString_masksSecretKey() {
        JwtProperties properties = new JwtProperties(SECRET_KEY, ACCESS_EXP_MILLIS, REFRESH_EXP_MILLIS, ISSUER);

        assertThat(properties.toString()).doesNotContain(SECRET_KEY);
    }

    @Test
    void blankSecretKey_throwsIllegalState() {
        assertThatThrownBy(() -> new JwtProperties(" ", ACCESS_EXP_MILLIS, REFRESH_EXP_MILLIS, ISSUER))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shortSecretKey_throwsIllegalState() {
        String shortSecretKey = generateSecretKey(32);

        assertThatThrownBy(() -> new JwtProperties(shortSecretKey, ACCESS_EXP_MILLIS, REFRESH_EXP_MILLIS, ISSUER))
            .isInstanceOf(IllegalStateException.class);
    }

}
