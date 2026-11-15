package com.chuseok22.mimefit.auth.core.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.chuseok22.mimefit.auth.core.constant.ClientType;
import com.chuseok22.mimefit.member.core.entity.Member;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class LoginSessionTest {

    private static final Instant NOW = Instant.parse("2026-11-15T01:00:00Z");
    private static final Instant EXPIRES_AT = Instant.parse("2026-12-15T01:00:00Z");

    private LoginSession createSession() {
        Member member = Member.create("firebase-uid", "user@example.com", null);
        return LoginSession.create(member, "device-1", ClientType.DESKTOP, "내 노트북", "hash-1", EXPIRES_AT, NOW);
    }

    @Test
    void matches_sameHash_true() {
        assertThat(createSession().matches("hash-1")).isTrue();
    }

    @Test
    void matches_differentHash_false() {
        assertThat(createSession().matches("hash-2")).isFalse();
    }

    @Test
    void isExpired_beforeExpiry_false() {
        assertThat(createSession().isExpired(EXPIRES_AT.minusSeconds(1))).isFalse();
    }

    @Test
    void isExpired_atOrAfterExpiry_true() {
        LoginSession session = createSession();

        assertThat(session.isExpired(EXPIRES_AT)).isTrue();
        assertThat(session.isExpired(EXPIRES_AT.plusSeconds(1))).isTrue();
    }

    @Test
    void rotate_replacesHashExpiryAndLastUsedAt() {
        LoginSession session = createSession();
        Instant later = NOW.plusSeconds(3600);
        Instant newExpiresAt = EXPIRES_AT.plusSeconds(3600);

        session.rotate("hash-2", newExpiresAt, later);

        assertThat(session.getRefreshTokenHash()).isEqualTo("hash-2");
        assertThat(session.getExpiresAt()).isEqualTo(newExpiresAt);
        assertThat(session.getLastUsedAt()).isEqualTo(later);
    }

    @Test
    void changeDevice_replacesClientTypeAndDeviceName() {
        LoginSession session = createSession();

        session.changeDevice(ClientType.WEBSITE, "크롬");

        assertThat(session.getClientType()).isEqualTo(ClientType.WEBSITE);
        assertThat(session.getDeviceName()).isEqualTo("크롬");
    }

}
