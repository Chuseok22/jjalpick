package com.chuseok22.jjalpick.auth.application.support;

import java.time.Duration;
import java.time.Instant;
import org.springframework.http.ResponseCookie;

/**
 * WEBSITE 리프레시 토큰 쿠키 규격과 생성·만료 헬퍼. 쿠키 속성은 이 클래스 한 곳에서만 관리한다.
 * application 계층이 infrastructure를 import할 수 없어 application에 둔다.
 */
public final class RefreshTokenCookie {

    public static final String NAME = "refreshToken";
    public static final String PATH = "/api/auth";
    public static final String SAME_SITE = "Strict";
    // 비워 두면 Domain 속성을 쓰지 않아 호스트 전용 쿠키가 된다
    public static final String DOMAIN = "";

    private RefreshTokenCookie() {
    }

    /**
     * 리프레시 토큰 쿠키를 만든다. Max-Age는 리프레시 토큰 만료까지 남은 초다.
     */
    public static ResponseCookie create(String refreshToken, Instant expiresAt, Instant now) {
        Duration maxAge = Duration.between(now, expiresAt);
        return baseBuilder(refreshToken).maxAge(maxAge.isNegative() ? Duration.ZERO : maxAge).build();
    }

    /**
     * 브라우저의 리프레시 토큰 쿠키를 즉시 만료시키는 쿠키를 만든다.
     */
    public static ResponseCookie expire() {
        return baseBuilder("").maxAge(Duration.ZERO).build();
    }

    private static ResponseCookie.ResponseCookieBuilder baseBuilder(String value) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(NAME, value)
            .path(PATH)
            .httpOnly(true)
            .secure(true)
            .sameSite(SAME_SITE);
        if (!DOMAIN.isEmpty()) {
            builder.domain(DOMAIN);
        }
        return builder;
    }

}
