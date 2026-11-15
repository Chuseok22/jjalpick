package com.chuseok22.mimefit.auth.core.port;

import java.util.UUID;

/**
 * 액세스·리프레시 토큰 발급과 검증 port.
 */
public interface TokenProvider {

    IssuedToken issueAccessToken(UUID memberId);

    IssuedToken issueRefreshToken(UUID memberId);

    /**
     * 액세스 토큰을 검증하고 회원 id를 돌려준다.
     * 만료면 ACCESS_TOKEN_EXPIRED, 그 외 모든 실패는 UNAUTHENTICATED.
     */
    UUID parseAccessToken(String token);

    /**
     * 리프레시 토큰을 검증하고 회원 id를 돌려준다. 만료를 포함한 모든 실패는 INVALID_REFRESH_TOKEN.
     */
    UUID parseRefreshToken(String token);

}
