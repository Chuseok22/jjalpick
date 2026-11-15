package com.chuseok22.mimefit.auth.application.service;

import com.chuseok22.mimefit.auth.core.constant.ClientType;
import java.time.Instant;

/**
 * 로그인·갱신 결과 토큰. 클라이언트 종류에 따라 컨트롤러가 응답 방식(본문 또는 쿠키)을 정한다.
 */
public record AuthTokens(
    String accessToken,
    Instant accessTokenExpiresAt,
    String refreshToken,
    Instant refreshTokenExpiresAt,
    ClientType clientType
) {

}
