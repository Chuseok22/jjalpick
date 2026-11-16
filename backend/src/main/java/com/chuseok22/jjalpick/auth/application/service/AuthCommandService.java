package com.chuseok22.jjalpick.auth.application.service;

import com.chuseok22.jjalpick.auth.core.port.IssuedToken;
import com.chuseok22.jjalpick.auth.core.port.TokenProvider;
import com.chuseok22.jjalpick.auth.core.port.VerifiedIdentity;
import com.chuseok22.jjalpick.auth.core.support.RefreshTokenHasher;
import com.chuseok22.jjalpick.common.core.exception.CustomException;
import com.chuseok22.jjalpick.common.core.exception.ErrorCode;
import com.chuseok22.jjalpick.member.core.entity.Member;
import com.chuseok22.jjalpick.member.core.repository.MemberRepository;
import java.time.Clock;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 로그인·토큰 갱신·로그아웃의 DB 처리를 담당한다. 외부 신원 검증은 {@link AuthService}에서 끝낸 뒤 호출한다.
 * 회원당 리프레시 토큰은 하나만 보관하며, 새로 로그인하면 이전 토큰을 덮어쓴다.
 * 동시 가입·동시 갱신에 대한 잠금은 두지 않는다(결정 사항).
 */
@Service
@RequiredArgsConstructor
public class AuthCommandService {

    private final MemberRepository memberRepository;
    private final TokenProvider tokenProvider;
    private final Clock clock;

    @Transactional
    public AuthTokens login(VerifiedIdentity identity) {
        Member member = memberRepository.findByFirebaseUidAndDeletedFalse(identity.firebaseUid())
            .orElseGet(() -> memberRepository.save(
                Member.create(identity.firebaseUid(), identity.email(), identity.name())));
        member.assertCanLogin();
        return issueTokens(member);
    }

    /**
     * 만료로 보관 토큰을 지운 뒤 예외를 던져도 삭제가 롤백되지 않도록 CustomException에는 롤백하지 않는다.
     * 보관 중인 토큰과 다른 토큰이 오면 보관 토큰은 그대로 두고 예외만 던진다(새 로그인은 이전 로그인만 끊는다).
     */
    @Transactional(noRollbackFor = CustomException.class)
    public AuthTokens refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        UUID memberId = tokenProvider.parseRefreshToken(refreshToken);
        Member member = memberRepository.findByIdAndDeletedFalse(memberId)
            .orElseThrow(() -> new CustomException(ErrorCode.INVALID_REFRESH_TOKEN));
        member.assertCanLogin();

        if (member.isRefreshTokenExpired(clock.instant())) {
            member.clearRefreshToken();
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        if (!member.matchesRefreshToken(RefreshTokenHasher.hash(refreshToken))) {
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        return issueTokens(member);
    }

    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        UUID memberId;
        try {
            memberId = tokenProvider.parseRefreshToken(refreshToken);
        } catch (CustomException exception) {
            // 로그아웃은 멱등이라 잘못된 토큰도 오류로 만들지 않는다(의도적으로 삼킨다).
            return;
        }
        memberRepository.findByIdAndDeletedFalse(memberId)
            .filter(member -> member.matchesRefreshToken(RefreshTokenHasher.hash(refreshToken)))
            .ifPresent(Member::clearRefreshToken);
    }

    private AuthTokens issueTokens(Member member) {
        IssuedToken accessToken = tokenProvider.issueAccessToken(member.getId());
        IssuedToken refreshToken = tokenProvider.issueRefreshToken(member.getId());
        member.startRefreshToken(RefreshTokenHasher.hash(refreshToken.token()), refreshToken.expiresAt());
        return new AuthTokens(accessToken.token(), accessToken.expiresAt(), refreshToken.token(),
            refreshToken.expiresAt());
    }

}
