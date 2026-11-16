package com.chuseok22.jjalpick.auth.application.dto.response;

import java.time.Instant;

/**
 * 로그인·갱신 응답.
 */
public record TokenResponse(
    String accessToken,
    Instant accessTokenExpiresAt,
    String refreshToken
) {

}
