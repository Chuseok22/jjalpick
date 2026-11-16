package com.chuseok22.jjalpick.auth.application.service;

import com.chuseok22.jjalpick.auth.application.dto.request.LoginRequest;
import com.chuseok22.jjalpick.auth.core.entity.LoginSession;
import com.chuseok22.jjalpick.auth.core.port.IssuedToken;
import com.chuseok22.jjalpick.auth.core.port.TokenProvider;
import com.chuseok22.jjalpick.auth.core.port.VerifiedIdentity;
import com.chuseok22.jjalpick.auth.core.repository.LoginSessionRepository;
import com.chuseok22.jjalpick.auth.core.support.RefreshTokenHasher;
import com.chuseok22.jjalpick.common.core.exception.CustomException;
import com.chuseok22.jjalpick.common.core.exception.ErrorCode;
import com.chuseok22.jjalpick.member.core.entity.Member;
import com.chuseok22.jjalpick.member.core.repository.MemberRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 로그인·토큰 갱신·로그아웃의 DB 처리를 담당한다. 외부 신원 검증은 {@link AuthService}에서 끝낸 뒤 호출한다.
 * 동시 가입·동시 갱신에 대한 잠금은 두지 않는다(결정 사항).
 */
@Service
@RequiredArgsConstructor
public class AuthCommandService {

    private final MemberRepository memberRepository;
    private final LoginSessionRepository loginSessionRepository;
    private final TokenProvider tokenProvider;
    private final Clock clock;

    @Transactional
    public AuthTokens login(LoginRequest request, VerifiedIdentity identity) {
        Member member = memberRepository.findByFirebaseUidAndDeletedFalse(identity.firebaseUid())
            .orElseGet(() -> memberRepository.save(
                Member.create(identity.firebaseUid(), identity.email(), identity.name())));
        member.assertCanLogin();

        Instant now = clock.instant();
        loginSessionRepository.deleteByMemberAndExpiresAtLessThanEqual(member, now);

        IssuedToken accessToken = tokenProvider.issueAccessToken(member.getId());
        IssuedToken refreshToken = tokenProvider.issueRefreshToken(member.getId());
        String refreshTokenHash = RefreshTokenHasher.hash(refreshToken.token());

        loginSessionRepository.findByMemberAndDeviceIdentifier(member, request.deviceIdentifier())
            .ifPresentOrElse(
                session -> {
                    session.rotate(refreshTokenHash, refreshToken.expiresAt(), now);
                    session.changeDevice(request.deviceName());
                },
                () -> loginSessionRepository.save(LoginSession.create(
                    member, request.deviceIdentifier(), request.deviceName(),
                    refreshTokenHash, refreshToken.expiresAt(), now))
            );
        return new AuthTokens(accessToken.token(), accessToken.expiresAt(), refreshToken.token(),
            refreshToken.expiresAt());
    }

    /**
     * 재사용·만료로 세션을 삭제한 뒤 예외를 던져도 삭제가 롤백되지 않도록 CustomException에는 롤백하지 않는다.
     */
    @Transactional(noRollbackFor = CustomException.class)
    public AuthTokens refresh(String refreshToken, String deviceIdentifier) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        UUID memberId = tokenProvider.parseRefreshToken(refreshToken);
        Member member = memberRepository.findByIdAndDeletedFalse(memberId)
            .orElseThrow(() -> new CustomException(ErrorCode.INVALID_REFRESH_TOKEN));
        member.assertCanLogin();

        LoginSession session = loginSessionRepository.findByMemberAndDeviceIdentifier(member, deviceIdentifier)
            .orElseThrow(() -> new CustomException(ErrorCode.INVALID_REFRESH_TOKEN));
        Instant now = clock.instant();
        String presentedTokenHash = RefreshTokenHasher.hash(refreshToken);
        if (!session.matches(presentedTokenHash)) {
            rejectMismatchedToken(session, presentedTokenHash);
        }
        if (session.isExpired(now)) {
            loginSessionRepository.delete(session);
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        IssuedToken newAccessToken = tokenProvider.issueAccessToken(memberId);
        IssuedToken newRefreshToken = tokenProvider.issueRefreshToken(memberId);
        session.rotate(RefreshTokenHasher.hash(newRefreshToken.token()), newRefreshToken.expiresAt(), now);
        return new AuthTokens(newAccessToken.token(), newAccessToken.expiresAt(), newRefreshToken.token(),
            newRefreshToken.expiresAt());
    }

    /**
     * 다른 기기의 현재 토큰이면 대상 세션을 지우지 않고 거부한다(남의 토큰으로 이 기기 세션을 지우는 것을 막는다).
     * 어느 세션에도 없는 토큰은 이미 교체된 토큰의 재사용으로 보고 대상 세션을 폐기한다.
     */
    private void rejectMismatchedToken(LoginSession targetSession, String presentedTokenHash) {
        boolean belongsToAnotherSession = loginSessionRepository.findByRefreshTokenHash(presentedTokenHash)
            .filter(found -> found != targetSession)
            .isPresent();
        if (!belongsToAnotherSession) {
            loginSessionRepository.delete(targetSession);
        }
        throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
    }

    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        loginSessionRepository.findByRefreshTokenHash(RefreshTokenHasher.hash(refreshToken))
            .ifPresent(loginSessionRepository::delete);
    }

}
