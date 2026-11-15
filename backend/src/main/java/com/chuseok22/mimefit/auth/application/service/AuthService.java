package com.chuseok22.mimefit.auth.application.service;

import com.chuseok22.mimefit.auth.application.dto.request.LoginRequest;
import com.chuseok22.mimefit.auth.core.port.IdentityVerifier;
import com.chuseok22.mimefit.auth.core.port.VerifiedIdentity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 인증 진입점. 트랜잭션을 열지 않으며, 외부 신원 검증 HTTP 호출 동안 DB 연결을 잡지 않도록
 * 검증을 끝낸 뒤 {@link AuthCommandService}의 트랜잭션에 위임한다.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final IdentityVerifier identityVerifier;
    private final AuthCommandService authCommandService;

    public AuthTokens login(LoginRequest request) {
        VerifiedIdentity identity = identityVerifier.verify(request.idToken());
        return authCommandService.login(request, identity);
    }

    public AuthTokens refresh(String refreshToken, String deviceIdentifier) {
        return authCommandService.refresh(refreshToken, deviceIdentifier);
    }

    public void logout(String refreshToken) {
        authCommandService.logout(refreshToken);
    }

}
