package com.chuseok22.mimefit.auth.infrastructure.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.chuseok22.mimefit.auth.core.port.VerifiedIdentity;
import com.chuseok22.mimefit.auth.infrastructure.properties.FirebaseProperties;
import com.chuseok22.mimefit.common.core.exception.CustomException;
import com.chuseok22.mimefit.common.core.exception.ErrorCode;
import com.google.firebase.auth.AuthErrorCode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FirebaseIdentityVerifierTest {

    private static final String ID_TOKEN = "test-id-token";

    private FirebaseAuth firebaseAuth;
    private FirebaseIdentityVerifier verifier;

    @BeforeEach
    void setUp() {
        firebaseAuth = mock(FirebaseAuth.class);
        verifier = new FirebaseIdentityVerifier(firebaseAuth);
    }

    private FirebaseToken tokenOf(String uid, String email, String name) {
        return tokenOf(uid, email, name, Map.of("firebase", Map.of("sign_in_provider", "google.com")));
    }

    private FirebaseToken tokenOf(String uid, String email, String name, Map<String, Object> claims) {
        FirebaseToken token = mock(FirebaseToken.class);
        when(token.getClaims()).thenReturn(claims);
        when(token.getUid()).thenReturn(uid);
        when(token.getEmail()).thenReturn(email);
        when(token.getName()).thenReturn(name);
        return token;
    }

    private void assertVerifyFailsWith(ErrorCode expected) {
        assertThatThrownBy(() -> verifier.verify(ID_TOKEN))
            .isInstanceOfSatisfying(CustomException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo(expected));
    }

    @Test
    void verify_validToken_returnsIdentity() throws Exception {
        FirebaseToken token = tokenOf("uid-1", "user@example.com", "사용자");
        when(firebaseAuth.verifyIdToken(ID_TOKEN, true)).thenReturn(token);

        VerifiedIdentity identity = verifier.verify(ID_TOKEN);

        assertThat(identity).isEqualTo(new VerifiedIdentity("uid-1", "user@example.com", "사용자"));
    }

    @Test
    void verify_tokenWithoutEmail_throwsInvalidIdentityToken() throws Exception {
        FirebaseToken token = tokenOf("uid-1", " ", "사용자");
        when(firebaseAuth.verifyIdToken(ID_TOKEN, true)).thenReturn(token);

        assertVerifyFailsWith(ErrorCode.INVALID_IDENTITY_TOKEN);
    }

    @Test
    void verify_tokenWithoutName_returnsIdentityWithNullName() throws Exception {
        FirebaseToken token = tokenOf("uid-1", "user@example.com", null);
        when(firebaseAuth.verifyIdToken(ID_TOKEN, true)).thenReturn(token);

        assertThat(verifier.verify(ID_TOKEN).name()).isNull();
    }

    @Test
    void verify_nonGoogleProvider_throwsInvalidIdentityToken() throws Exception {
        FirebaseToken token = tokenOf("uid-1", "user@example.com", "사용자",
            Map.of("firebase", Map.of("sign_in_provider", "password")));
        when(firebaseAuth.verifyIdToken(ID_TOKEN, true)).thenReturn(token);

        assertVerifyFailsWith(ErrorCode.INVALID_IDENTITY_TOKEN);
    }

    @Test
    void verify_missingProviderClaim_throwsInvalidIdentityToken() throws Exception {
        FirebaseToken token = tokenOf("uid-1", "user@example.com", "사용자", Map.of());
        when(firebaseAuth.verifyIdToken(ID_TOKEN, true)).thenReturn(token);

        assertVerifyFailsWith(ErrorCode.INVALID_IDENTITY_TOKEN);
    }

    @Test
    void verify_authException_throwsInvalidIdentityToken() throws Exception {
        FirebaseAuthException authException = mock(FirebaseAuthException.class);
        when(authException.getAuthErrorCode()).thenReturn(AuthErrorCode.EXPIRED_ID_TOKEN);
        when(firebaseAuth.verifyIdToken(ID_TOKEN, true)).thenThrow(authException);

        assertVerifyFailsWith(ErrorCode.INVALID_IDENTITY_TOKEN);
    }

    @Test
    void verify_certificateFetchFailed_throwsIdentityProviderUnavailable() throws Exception {
        FirebaseAuthException authException = mock(FirebaseAuthException.class);
        when(authException.getAuthErrorCode()).thenReturn(AuthErrorCode.CERTIFICATE_FETCH_FAILED);
        when(firebaseAuth.verifyIdToken(ID_TOKEN, true)).thenThrow(authException);

        assertVerifyFailsWith(ErrorCode.IDENTITY_PROVIDER_UNAVAILABLE);
    }

    @Test
    void blankLocation_throwsIllegalState() {
        assertThatThrownBy(() -> new FirebaseProperties(" ")).isInstanceOf(IllegalStateException.class);
    }

}
