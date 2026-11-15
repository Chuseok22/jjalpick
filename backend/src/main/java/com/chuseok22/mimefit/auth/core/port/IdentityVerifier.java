package com.chuseok22.mimefit.auth.core.port;

/**
 * 외부 신원 제공자의 ID 토큰 검증 port.
 */
public interface IdentityVerifier {

    VerifiedIdentity verify(String idToken);

}
