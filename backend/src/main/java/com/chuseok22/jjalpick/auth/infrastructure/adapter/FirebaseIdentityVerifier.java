package com.chuseok22.jjalpick.auth.infrastructure.adapter;

import com.chuseok22.jjalpick.auth.core.port.IdentityVerifier;
import com.chuseok22.jjalpick.auth.core.port.VerifiedIdentity;
import com.chuseok22.jjalpick.common.core.exception.CustomException;
import com.chuseok22.jjalpick.common.core.exception.ErrorCode;
import com.google.firebase.auth.AuthErrorCode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Firebase ID 토큰을 Admin SDK로 검증한다. 토큰 값, uid, 이메일은 로그에 남기지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FirebaseIdentityVerifier implements IdentityVerifier {

    private static final String FIREBASE_CLAIM = "firebase";
    private static final String SIGN_IN_PROVIDER_KEY = "sign_in_provider";
    private static final String GOOGLE_PROVIDER = "google.com";

    private final FirebaseAuth firebaseAuth;

    @Override
    public VerifiedIdentity verify(String idToken) {
        FirebaseToken token = verifyToken(idToken);
        requireGoogleProvider(token);
        String email = token.getEmail();
        if (email == null || email.isBlank()) {
            log.warn("신원 토큰 거절: reason=EMAIL_MISSING");
            throw new CustomException(ErrorCode.INVALID_IDENTITY_TOKEN);
        }
        return new VerifiedIdentity(token.getUid(), email, token.getName());
    }

    // Google 로그인만 허용한다. 클레임이 없거나 다른 제공자면 거절한다
    private void requireGoogleProvider(FirebaseToken token) {
        Map<String, Object> claims = token.getClaims();
        Object firebaseClaim = claims == null ? null : claims.get(FIREBASE_CLAIM);
        Object provider = firebaseClaim instanceof Map<?, ?> claim ? claim.get(SIGN_IN_PROVIDER_KEY) : null;
        if (!GOOGLE_PROVIDER.equals(provider)) {
            log.warn("신원 토큰 거절: reason=UNSUPPORTED_PROVIDER");
            throw new CustomException(ErrorCode.INVALID_IDENTITY_TOKEN);
        }
    }

    // 서명·만료·대상 프로젝트에 더해 폐기(revoke)와 비활성 사용자도 확인한다
    private FirebaseToken verifyToken(String idToken) {
        try {
            return firebaseAuth.verifyIdToken(idToken, true);
        } catch (FirebaseAuthException exception) {
            AuthErrorCode authErrorCode = exception.getAuthErrorCode();
            if (authErrorCode == null || authErrorCode == AuthErrorCode.CERTIFICATE_FETCH_FAILED) {
                log.error("Firebase 토큰 검증 서버 오류: authErrorCode={}", authErrorCode, exception);
                throw new CustomException(ErrorCode.IDENTITY_PROVIDER_UNAVAILABLE, exception);
            }
            log.warn("Firebase 토큰 검증 실패: authErrorCode={}", authErrorCode);
            throw new CustomException(ErrorCode.INVALID_IDENTITY_TOKEN, exception);
        }
    }

}
