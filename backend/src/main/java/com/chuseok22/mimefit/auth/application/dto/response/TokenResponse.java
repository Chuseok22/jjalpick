package com.chuseok22.mimefit.auth.application.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

/**
 * 로그인·갱신 응답. WEBSITE는 리프레시 토큰을 쿠키로만 받으므로 {@code refreshToken}이 null이고 필드가 생략된다.
 */
public record TokenResponse(
    String accessToken,
    Instant accessTokenExpiresAt,
    @JsonInclude(JsonInclude.Include.NON_NULL) String refreshToken
) {

}
