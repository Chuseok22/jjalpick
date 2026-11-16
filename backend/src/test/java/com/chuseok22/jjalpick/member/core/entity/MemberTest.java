package com.chuseok22.jjalpick.member.core.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.chuseok22.jjalpick.common.core.exception.CustomException;
import com.chuseok22.jjalpick.common.core.exception.ErrorCode;
import com.chuseok22.jjalpick.member.core.constant.MemberStatus;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class MemberTest {

    @Test
    void create_newMember_isActive() {
        Member member = Member.create("firebase-uid", "user@example.com", "홍길동");

        assertThat(member.getStatus()).isEqualTo(MemberStatus.ACTIVE);
        assertThat(member.getFirebaseUid()).isEqualTo("firebase-uid");
        assertThat(member.getEmail()).isEqualTo("user@example.com");
        assertThat(member.getName()).isEqualTo("홍길동");
    }

    @Test
    void assertCanLogin_activeMember_doesNotThrow() {
        Member member = Member.create("firebase-uid", "user@example.com", null);

        assertThatCode(member::assertCanLogin).doesNotThrowAnyException();
    }

    @Test
    void assertCanLogin_suspendedMember_throwsMemberSuspended() {
        Member member = Member.create("firebase-uid", "user@example.com", null);
        ReflectionTestUtils.setField(member, "status", MemberStatus.SUSPENDED);

        assertThatThrownBy(member::assertCanLogin)
            .isInstanceOf(CustomException.class)
            .extracting(exception -> ((CustomException) exception).getErrorCode())
            .isEqualTo(ErrorCode.MEMBER_SUSPENDED);
    }

    @Test
    void startRefreshToken_overwritesPreviousToken() {
        Member member = Member.create("firebase-uid", "user@example.com", null);
        Instant expiresAt = Instant.parse("2026-12-01T00:00:00Z");
        member.startRefreshToken("first-hash", expiresAt);

        member.startRefreshToken("second-hash", expiresAt.plusSeconds(60));

        assertThat(member.matchesRefreshToken("first-hash")).isFalse();
        assertThat(member.matchesRefreshToken("second-hash")).isTrue();
        assertThat(member.getRefreshTokenExpiresAt()).isEqualTo(expiresAt.plusSeconds(60));
    }

    @Test
    void matchesRefreshToken_withoutStoredToken_returnsFalse() {
        Member member = Member.create("firebase-uid", "user@example.com", null);

        assertThat(member.matchesRefreshToken("any-hash")).isFalse();
    }

    @Test
    void clearRefreshToken_removesHashAndExpiry() {
        Member member = Member.create("firebase-uid", "user@example.com", null);
        member.startRefreshToken("hash", Instant.parse("2026-12-01T00:00:00Z"));

        member.clearRefreshToken();

        assertThat(member.matchesRefreshToken("hash")).isFalse();
        assertThat(member.isRefreshTokenExpired(Instant.parse("2026-11-01T00:00:00Z"))).isTrue();
    }

    @Test
    void isRefreshTokenExpired_atExactExpiry_returnsTrue() {
        Member member = Member.create("firebase-uid", "user@example.com", null);
        Instant expiresAt = Instant.parse("2026-12-01T00:00:00Z");
        member.startRefreshToken("hash", expiresAt);

        assertThat(member.isRefreshTokenExpired(expiresAt)).isTrue();
        assertThat(member.isRefreshTokenExpired(expiresAt.minusSeconds(1))).isFalse();
    }

}
