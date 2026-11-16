package com.chuseok22.jjalpick.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.chuseok22.jjalpick.auth.core.port.IssuedToken;
import com.chuseok22.jjalpick.auth.core.port.TokenProvider;
import com.chuseok22.jjalpick.auth.core.port.VerifiedIdentity;
import com.chuseok22.jjalpick.auth.core.support.RefreshTokenHasher;
import com.chuseok22.jjalpick.common.core.exception.CustomException;
import com.chuseok22.jjalpick.common.core.exception.ErrorCode;
import com.chuseok22.jjalpick.member.core.constant.MemberStatus;
import com.chuseok22.jjalpick.member.core.entity.Member;
import com.chuseok22.jjalpick.member.core.repository.MemberRepository;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

class AuthCommandServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-15T00:00:00Z");
    private static final String CURRENT_REFRESH_TOKEN = "current-refresh-token";
    private static final VerifiedIdentity IDENTITY = new VerifiedIdentity("firebase-uid", "member@example.com", "회원");

    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final TokenProvider tokenProvider = mock(TokenProvider.class);
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    private AuthCommandService authCommandService;
    private Member member;
    private UUID memberId;

    @BeforeEach
    void setUp() {
        authCommandService = new AuthCommandService(memberRepository, tokenProvider, clock);
        memberId = UUID.randomUUID();
        member = Member.create(IDENTITY.firebaseUid(), IDENTITY.email(), IDENTITY.name());
        ReflectionTestUtils.setField(member, "id", memberId);
        given(tokenProvider.issueAccessToken(memberId))
            .willReturn(new IssuedToken("new-access", NOW.plus(Duration.ofMinutes(15))));
        given(tokenProvider.issueRefreshToken(memberId))
            .willReturn(new IssuedToken("new-refresh", NOW.plus(Duration.ofDays(30))));
        given(memberRepository.save(any(Member.class))).willAnswer(invocation -> {
            Member saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", memberId);
            return saved;
        });
    }

    private void givenStoredRefreshToken(Instant expiresAt) {
        member.startRefreshToken(RefreshTokenHasher.hash(CURRENT_REFRESH_TOKEN), expiresAt);
    }

    private void givenParsableRefreshToken() {
        given(tokenProvider.parseRefreshToken(CURRENT_REFRESH_TOKEN)).willReturn(memberId);
        given(memberRepository.findByIdAndDeletedFalse(memberId)).willReturn(Optional.of(member));
    }

    @Test
    void login_newMember_createsMemberAndStoresRefreshTokenHash() {
        given(memberRepository.findByFirebaseUidAndDeletedFalse("firebase-uid")).willReturn(Optional.empty());

        AuthTokens tokens = authCommandService.login(IDENTITY);

        ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
        verify(memberRepository).save(memberCaptor.capture());
        Member savedMember = memberCaptor.getValue();
        assertThat(savedMember.getFirebaseUid()).isEqualTo(IDENTITY.firebaseUid());
        assertThat(savedMember.getEmail()).isEqualTo(IDENTITY.email());
        assertThat(savedMember.getName()).isEqualTo(IDENTITY.name());
        assertThat(savedMember.matchesRefreshToken(RefreshTokenHasher.hash("new-refresh"))).isTrue();
        assertThat(savedMember.getRefreshTokenExpiresAt()).isEqualTo(NOW.plus(Duration.ofDays(30)));
        assertThat(tokens.accessToken()).isEqualTo("new-access");
        assertThat(tokens.refreshToken()).isEqualTo("new-refresh");
    }

    @Test
    void login_existingMember_doesNotCreateMember() {
        given(memberRepository.findByFirebaseUidAndDeletedFalse("firebase-uid")).willReturn(Optional.of(member));

        authCommandService.login(IDENTITY);

        verify(memberRepository, never()).save(any(Member.class));
        assertThat(member.matchesRefreshToken(RefreshTokenHasher.hash("new-refresh"))).isTrue();
    }

    @Test
    void login_suspendedMember_throwsMemberSuspended() {
        ReflectionTestUtils.setField(member, "status", MemberStatus.SUSPENDED);
        given(memberRepository.findByFirebaseUidAndDeletedFalse("firebase-uid")).willReturn(Optional.of(member));

        assertThatThrownBy(() -> authCommandService.login(IDENTITY))
            .isInstanceOfSatisfying(CustomException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MEMBER_SUSPENDED));
        verifyNoInteractions(tokenProvider);
    }

    @Test
    void login_secondLogin_overwritesPreviousRefreshTokenHash() {
        given(tokenProvider.issueRefreshToken(memberId))
            .willReturn(new IssuedToken("first-refresh", NOW.plus(Duration.ofDays(30))))
            .willReturn(new IssuedToken("second-refresh", NOW.plus(Duration.ofDays(30))));
        given(memberRepository.findByFirebaseUidAndDeletedFalse("firebase-uid")).willReturn(Optional.of(member));

        authCommandService.login(IDENTITY);
        authCommandService.login(IDENTITY);

        assertThat(member.matchesRefreshToken(RefreshTokenHasher.hash("first-refresh"))).isFalse();
        assertThat(member.matchesRefreshToken(RefreshTokenHasher.hash("second-refresh"))).isTrue();
    }

    @Test
    void refresh_matchingToken_rotatesAndReturnsNewTokens() {
        givenStoredRefreshToken(NOW.plus(Duration.ofDays(1)));
        givenParsableRefreshToken();

        AuthTokens tokens = authCommandService.refresh(CURRENT_REFRESH_TOKEN);

        assertThat(tokens.accessToken()).isEqualTo("new-access");
        assertThat(tokens.refreshToken()).isEqualTo("new-refresh");
        assertThat(tokens.refreshTokenExpiresAt()).isEqualTo(NOW.plus(Duration.ofDays(30)));
        assertThat(member.matchesRefreshToken(RefreshTokenHasher.hash("new-refresh"))).isTrue();
        assertThat(member.matchesRefreshToken(RefreshTokenHasher.hash(CURRENT_REFRESH_TOKEN))).isFalse();
    }

    @Test
    void refresh_mismatchedToken_keepsStoredTokenAndThrowsInvalidRefreshToken() {
        Instant storedExpiresAt = NOW.plus(Duration.ofDays(1));
        String storedHash = RefreshTokenHasher.hash("other-token");
        member.startRefreshToken(storedHash, storedExpiresAt);
        givenParsableRefreshToken();

        assertInvalidRefreshToken(() -> authCommandService.refresh(CURRENT_REFRESH_TOKEN));
        assertThat(member.getRefreshTokenHash()).isEqualTo(storedHash);
        assertThat(member.getRefreshTokenExpiresAt()).isEqualTo(storedExpiresAt);
    }

    @Test
    void refresh_afterSecondLogin_staleTokenFailsAndNewTokenStaysValid() {
        Instant expiresAt = NOW.plus(Duration.ofDays(1));
        member.startRefreshToken(RefreshTokenHasher.hash(CURRENT_REFRESH_TOKEN), expiresAt);
        member.startRefreshToken(RefreshTokenHasher.hash("second-login-token"), expiresAt);
        givenParsableRefreshToken();

        assertInvalidRefreshToken(() -> authCommandService.refresh(CURRENT_REFRESH_TOKEN));
        assertThat(member.matchesRefreshToken(RefreshTokenHasher.hash("second-login-token"))).isTrue();
    }

    @Test
    void refresh_storedExpiryPassed_clearsStoredTokenAndThrowsInvalidRefreshToken() {
        givenStoredRefreshToken(NOW);
        givenParsableRefreshToken();

        assertInvalidRefreshToken(() -> authCommandService.refresh(CURRENT_REFRESH_TOKEN));
        assertThat(member.getRefreshTokenHash()).isNull();
        assertThat(member.getRefreshTokenExpiresAt()).isNull();
    }

    @Test
    void refresh_withoutStoredToken_throwsInvalidRefreshToken() {
        givenParsableRefreshToken();

        assertInvalidRefreshToken(() -> authCommandService.refresh(CURRENT_REFRESH_TOKEN));
        assertThat(member.getRefreshTokenHash()).isNull();
    }

    @Test
    void refresh_unknownMember_throwsInvalidRefreshToken() {
        given(tokenProvider.parseRefreshToken(CURRENT_REFRESH_TOKEN)).willReturn(memberId);
        given(memberRepository.findByIdAndDeletedFalse(memberId)).willReturn(Optional.empty());

        assertInvalidRefreshToken(() -> authCommandService.refresh(CURRENT_REFRESH_TOKEN));
    }

    @Test
    void refresh_suspendedMember_throwsMemberSuspended() {
        ReflectionTestUtils.setField(member, "status", MemberStatus.SUSPENDED);
        givenStoredRefreshToken(NOW.plus(Duration.ofDays(1)));
        givenParsableRefreshToken();

        assertThatThrownBy(() -> authCommandService.refresh(CURRENT_REFRESH_TOKEN))
            .isInstanceOfSatisfying(CustomException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MEMBER_SUSPENDED));
    }

    @Test
    void refresh_blankToken_throwsInvalidRefreshToken() {
        assertInvalidRefreshToken(() -> authCommandService.refresh(" "));
        assertInvalidRefreshToken(() -> authCommandService.refresh(null));
        verifyNoInteractions(tokenProvider, memberRepository);
    }

    @Test
    void refresh_declaresNoRollbackForCustomException() {
        Method refreshMethod = Arrays.stream(AuthCommandService.class.getDeclaredMethods())
            .filter(method -> method.getName().equals("refresh"))
            .findFirst()
            .orElseThrow();

        Transactional transactional = refreshMethod.getAnnotation(Transactional.class);

        assertThat(transactional).isNotNull();
        assertThat(transactional.noRollbackFor()).contains(CustomException.class);
    }

    @Test
    void logout_matchingToken_clearsStoredToken() {
        givenStoredRefreshToken(NOW.plus(Duration.ofDays(1)));
        givenParsableRefreshToken();

        authCommandService.logout(CURRENT_REFRESH_TOKEN);

        assertThat(member.getRefreshTokenHash()).isNull();
        assertThat(member.getRefreshTokenExpiresAt()).isNull();
    }

    @Test
    void logout_mismatchedToken_keepsStoredToken() {
        member.startRefreshToken(RefreshTokenHasher.hash("other-token"), NOW.plus(Duration.ofDays(1)));
        givenParsableRefreshToken();

        authCommandService.logout(CURRENT_REFRESH_TOKEN);

        assertThat(member.matchesRefreshToken(RefreshTokenHasher.hash("other-token"))).isTrue();
    }

    @Test
    void logout_invalidToken_doesNothing() {
        given(tokenProvider.parseRefreshToken("invalid-token"))
            .willThrow(new CustomException(ErrorCode.INVALID_REFRESH_TOKEN));

        authCommandService.logout("invalid-token");

        verifyNoInteractions(memberRepository);
    }

    @Test
    void logout_blankToken_doesNothing() {
        authCommandService.logout(" ");
        authCommandService.logout(null);

        verifyNoInteractions(tokenProvider, memberRepository);
    }

    private void assertInvalidRefreshToken(Runnable action) {
        assertThatThrownBy(action::run)
            .isInstanceOfSatisfying(CustomException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN));
    }

}
