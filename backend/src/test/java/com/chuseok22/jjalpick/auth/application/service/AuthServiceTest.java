package com.chuseok22.jjalpick.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.chuseok22.jjalpick.auth.application.dto.request.LoginRequest;
import com.chuseok22.jjalpick.auth.core.port.IdentityVerifier;
import com.chuseok22.jjalpick.auth.core.port.VerifiedIdentity;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class AuthServiceTest {

    private final IdentityVerifier identityVerifier = mock(IdentityVerifier.class);
    private final AuthCommandService authCommandService = mock(AuthCommandService.class);
    private final AuthService authService = new AuthService(identityVerifier, authCommandService);

    @Test
    void login_verifiesIdentityThenDelegatesToCommandService() {
        LoginRequest request = new LoginRequest("id-token");
        VerifiedIdentity identity = new VerifiedIdentity("firebase-uid", "member@example.com", "회원");
        AuthTokens expected = new AuthTokens("access", Instant.EPOCH, "refresh", Instant.EPOCH);
        given(identityVerifier.verify("id-token")).willReturn(identity);
        given(authCommandService.login(identity)).willReturn(expected);

        AuthTokens actual = authService.login(request);

        assertThat(actual).isSameAs(expected);
        InOrder inOrder = inOrder(identityVerifier, authCommandService);
        inOrder.verify(identityVerifier).verify("id-token");
        inOrder.verify(authCommandService).login(identity);
    }

    @Test
    void refresh_delegatesToCommandService() {
        AuthTokens expected = new AuthTokens("access", Instant.EPOCH, "refresh", Instant.EPOCH);
        given(authCommandService.refresh("token")).willReturn(expected);

        assertThat(authService.refresh("token")).isSameAs(expected);
    }

    @Test
    void logout_delegatesToCommandService() {
        authService.logout("token");

        verify(authCommandService).logout("token");
    }

}
