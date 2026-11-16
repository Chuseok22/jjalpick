package com.chuseok22.jjalpick.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.chuseok22.jjalpick.auth.application.dto.request.LoginRequest;
import com.chuseok22.jjalpick.auth.core.entity.LoginSession;
import com.chuseok22.jjalpick.auth.core.port.IssuedToken;
import com.chuseok22.jjalpick.auth.core.port.TokenProvider;
import com.chuseok22.jjalpick.auth.core.port.VerifiedIdentity;
import com.chuseok22.jjalpick.auth.core.repository.LoginSessionRepository;
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
    private static final String DEVICE_IDENTIFIER = "device-1";
    private static final String CURRENT_REFRESH_TOKEN = "current-refresh-token";
    private static final VerifiedIdentity IDENTITY = new VerifiedIdentity("firebase-uid", "member@example.com", "회원");

    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final LoginSessionRepository loginSessionRepository = mock(LoginSessionRepository.class);
    private final TokenProvider tokenProvider = mock(TokenProvider.class);
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    private AuthCommandService authCommandService;
    private Member member;
    private UUID memberId;

    @BeforeEach
    void setUp() {
        authCommandService = new AuthCommandService(memberRepository, loginSessionRepository, tokenProvider, clock);
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

    private LoginRequest loginRequest() {
        return new LoginRequest("id-token", DEVICE_IDENTIFIER, "내 PC");
    }

    private LoginSession existingSession(Instant expiresAt) {
        return LoginSession.create(member, DEVICE_IDENTIFIER, "옛 기기",
            RefreshTokenHasher.hash(CURRENT_REFRESH_TOKEN), expiresAt, NOW.minusSeconds(60));
    }

    private void givenValidRefreshToken(LoginSession session) {
        given(tokenProvider.parseRefreshToken(CURRENT_REFRESH_TOKEN)).willReturn(memberId);
        given(memberRepository.findByIdAndDeletedFalse(memberId)).willReturn(Optional.of(member));
        given(loginSessionRepository.findByMemberAndDeviceIdentifier(member, DEVICE_IDENTIFIER))
            .willReturn(Optional.ofNullable(session));
    }

    @Test
    void login_newMember_createsMemberAndSession() {
        given(memberRepository.findByFirebaseUidAndDeletedFalse("firebase-uid")).willReturn(Optional.empty());
        given(loginSessionRepository.findByMemberAndDeviceIdentifier(any(Member.class), any()))
            .willReturn(Optional.empty());

        AuthTokens tokens = authCommandService.login(loginRequest(), IDENTITY);

        ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
        verify(memberRepository).save(memberCaptor.capture());
        Member savedMember = memberCaptor.getValue();
        assertThat(savedMember.getFirebaseUid()).isEqualTo(IDENTITY.firebaseUid());
        assertThat(savedMember.getEmail()).isEqualTo(IDENTITY.email());
        assertThat(savedMember.getName()).isEqualTo(IDENTITY.name());
        ArgumentCaptor<LoginSession> captor = ArgumentCaptor.forClass(LoginSession.class);
        verify(loginSessionRepository).save(captor.capture());
        LoginSession saved = captor.getValue();
        assertThat(saved.getMember()).isSameAs(savedMember);
        assertThat(saved.getDeviceIdentifier()).isEqualTo(DEVICE_IDENTIFIER);
        assertThat(saved.getRefreshTokenHash()).isEqualTo(RefreshTokenHasher.hash("new-refresh"));
        assertThat(tokens.accessToken()).isEqualTo("new-access");
        assertThat(tokens.refreshToken()).isEqualTo("new-refresh");
    }

    @Test
    void login_existingMember_doesNotCreateMember() {
        given(memberRepository.findByFirebaseUidAndDeletedFalse("firebase-uid")).willReturn(Optional.of(member));
        given(loginSessionRepository.findByMemberAndDeviceIdentifier(member, DEVICE_IDENTIFIER))
            .willReturn(Optional.empty());

        authCommandService.login(loginRequest(), IDENTITY);

        verify(memberRepository, never()).save(any(Member.class));
        verify(loginSessionRepository).save(any(LoginSession.class));
    }

    @Test
    void login_suspendedMember_throwsMemberSuspended() {
        ReflectionTestUtils.setField(member, "status", MemberStatus.SUSPENDED);
        given(memberRepository.findByFirebaseUidAndDeletedFalse("firebase-uid")).willReturn(Optional.of(member));

        assertThatThrownBy(() -> authCommandService.login(loginRequest(), IDENTITY))
            .isInstanceOfSatisfying(CustomException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MEMBER_SUSPENDED));
        verifyNoInteractions(loginSessionRepository);
    }

    @Test
    void login_sameDevice_rotatesExistingSession() {
        LoginSession session = existingSession(NOW.plus(Duration.ofDays(1)));
        given(memberRepository.findByFirebaseUidAndDeletedFalse("firebase-uid")).willReturn(Optional.of(member));
        given(loginSessionRepository.findByMemberAndDeviceIdentifier(member, DEVICE_IDENTIFIER))
            .willReturn(Optional.of(session));

        authCommandService.login(loginRequest(), IDENTITY);

        verify(loginSessionRepository, never()).save(any(LoginSession.class));
        assertThat(session.getRefreshTokenHash()).isEqualTo(RefreshTokenHasher.hash("new-refresh"));
        assertThat(session.getExpiresAt()).isEqualTo(NOW.plus(Duration.ofDays(30)));
        assertThat(session.getDeviceName()).isEqualTo("내 PC");
    }

    @Test
    void login_deletesExpiredSessionsOfMember() {
        given(memberRepository.findByFirebaseUidAndDeletedFalse("firebase-uid")).willReturn(Optional.of(member));
        given(loginSessionRepository.findByMemberAndDeviceIdentifier(member, DEVICE_IDENTIFIER))
            .willReturn(Optional.empty());

        authCommandService.login(loginRequest(), IDENTITY);

        verify(loginSessionRepository).deleteByMemberAndExpiresAtLessThanEqual(member, NOW);
    }

    @Test
    void refresh_matchingToken_rotatesAndReturnsNewTokens() {
        LoginSession session = existingSession(NOW.plus(Duration.ofDays(1)));
        givenValidRefreshToken(session);

        AuthTokens tokens = authCommandService.refresh(CURRENT_REFRESH_TOKEN, DEVICE_IDENTIFIER);

        assertThat(tokens.accessToken()).isEqualTo("new-access");
        assertThat(tokens.refreshToken()).isEqualTo("new-refresh");
        assertThat(tokens.refreshTokenExpiresAt()).isEqualTo(NOW.plus(Duration.ofDays(30)));
        assertThat(session.getRefreshTokenHash()).isEqualTo(RefreshTokenHasher.hash("new-refresh"));
        assertThat(session.getLastUsedAt()).isEqualTo(NOW);
        verify(loginSessionRepository, never()).delete(any(LoginSession.class));
    }

    @Test
    void refresh_reusedToken_deletesSessionAndThrowsInvalidRefreshToken() {
        LoginSession session = LoginSession.create(member, DEVICE_IDENTIFIER, "기기",
            RefreshTokenHasher.hash("other-token"), NOW.plus(Duration.ofDays(1)), NOW);
        givenValidRefreshToken(session);

        assertInvalidRefreshToken(() -> authCommandService.refresh(CURRENT_REFRESH_TOKEN, DEVICE_IDENTIFIER));
        verify(loginSessionRepository).delete(session);
    }

    @Test
    void refresh_currentTokenOfAnotherDevice_throwsAndKeepsTargetSession() {
        LoginSession targetSession = LoginSession.create(member, DEVICE_IDENTIFIER, "기기",
            RefreshTokenHasher.hash("other-token"), NOW.plus(Duration.ofDays(1)), NOW);
        LoginSession otherDeviceSession = LoginSession.create(member, "device-2", "다른 기기",
            RefreshTokenHasher.hash(CURRENT_REFRESH_TOKEN), NOW.plus(Duration.ofDays(1)), NOW);
        givenValidRefreshToken(targetSession);
        given(loginSessionRepository.findByRefreshTokenHash(RefreshTokenHasher.hash(CURRENT_REFRESH_TOKEN)))
            .willReturn(Optional.of(otherDeviceSession));

        assertInvalidRefreshToken(() -> authCommandService.refresh(CURRENT_REFRESH_TOKEN, DEVICE_IDENTIFIER));
        verify(loginSessionRepository, never()).delete(any(LoginSession.class));
    }

    @Test
    void refresh_expiredSession_deletesSessionAndThrowsInvalidRefreshToken() {
        LoginSession session = existingSession(NOW);
        givenValidRefreshToken(session);

        assertInvalidRefreshToken(() -> authCommandService.refresh(CURRENT_REFRESH_TOKEN, DEVICE_IDENTIFIER));
        verify(loginSessionRepository).delete(session);
    }

    @Test
    void refresh_unknownDevice_throwsInvalidRefreshToken() {
        givenValidRefreshToken(null);

        assertInvalidRefreshToken(() -> authCommandService.refresh(CURRENT_REFRESH_TOKEN, DEVICE_IDENTIFIER));
        verify(loginSessionRepository, never()).delete(any(LoginSession.class));
    }

    @Test
    void refresh_suspendedMember_throwsMemberSuspended() {
        ReflectionTestUtils.setField(member, "status", MemberStatus.SUSPENDED);
        givenValidRefreshToken(existingSession(NOW.plus(Duration.ofDays(1))));

        assertThatThrownBy(() -> authCommandService.refresh(CURRENT_REFRESH_TOKEN, DEVICE_IDENTIFIER))
            .isInstanceOfSatisfying(CustomException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MEMBER_SUSPENDED));
    }

    @Test
    void refresh_blankToken_throwsInvalidRefreshToken() {
        assertInvalidRefreshToken(() -> authCommandService.refresh(" ", DEVICE_IDENTIFIER));
        assertInvalidRefreshToken(() -> authCommandService.refresh(null, DEVICE_IDENTIFIER));
        verifyNoInteractions(tokenProvider, memberRepository, loginSessionRepository);
    }

    @Test
    void logout_knownToken_deletesSession() {
        LoginSession session = existingSession(NOW.plus(Duration.ofDays(1)));
        given(loginSessionRepository.findByRefreshTokenHash(RefreshTokenHasher.hash(CURRENT_REFRESH_TOKEN)))
            .willReturn(Optional.of(session));

        authCommandService.logout(CURRENT_REFRESH_TOKEN);

        verify(loginSessionRepository).delete(session);
    }

    @Test
    void logout_unknownToken_doesNothing() {
        given(loginSessionRepository.findByRefreshTokenHash(any())).willReturn(Optional.empty());

        authCommandService.logout("unknown-token");

        verify(loginSessionRepository, never()).delete(any(LoginSession.class));
    }

    @Test
    void logout_blankToken_doesNothing() {
        authCommandService.logout(" ");
        authCommandService.logout(null);

        verifyNoInteractions(loginSessionRepository);
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

    private void assertInvalidRefreshToken(Runnable action) {
        assertThatThrownBy(action::run)
            .isInstanceOfSatisfying(CustomException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN));
    }

}
