package com.chuseok22.jjalpick.auth.application.service;

import java.time.Instant;

/**
 * 로그인·갱신 결과 토큰. 리프레시 토큰은 응답 본문으로 내려준다.
 */
public record AuthTokens(
    String accessToken,
    Instant accessTokenExpiresAt,
    String refreshToken,
    Instant refreshTokenExpiresAt
) {

}
