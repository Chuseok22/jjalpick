package com.chuseok22.jjalpick.auth.core.entity;

import com.chuseok22.jjalpick.auth.core.constant.ClientType;
import com.chuseok22.jjalpick.common.core.entity.BaseEntity;
import com.chuseok22.jjalpick.member.core.entity.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원의 기기별 로그인 세션. 기기당 한 행만 유지하며 이력이 필요 없어 soft delete 대신 행을 삭제한다
 * (soft delete 규칙의 예외).
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "login_session")
public class LoginSession extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false)
    private String deviceIdentifier;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClientType clientType;

    @Column(nullable = false)
    private String deviceName;

    @Column(nullable = false)
    private String refreshTokenHash;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private Instant lastUsedAt;

    public static LoginSession create(
        Member member,
        String deviceIdentifier,
        ClientType clientType,
        String deviceName,
        String refreshTokenHash,
        Instant expiresAt,
        Instant now
    ) {
        LoginSession loginSession = new LoginSession();
        loginSession.member = member;
        loginSession.deviceIdentifier = deviceIdentifier;
        loginSession.clientType = clientType;
        loginSession.deviceName = deviceName;
        loginSession.refreshTokenHash = refreshTokenHash;
        loginSession.expiresAt = expiresAt;
        loginSession.lastUsedAt = now;
        return loginSession;
    }

    /**
     * 리프레시 토큰을 교체하고 만료 시각과 마지막 사용 시각을 갱신한다.
     */
    public void rotate(String refreshTokenHash, Instant expiresAt, Instant now) {
        this.refreshTokenHash = refreshTokenHash;
        this.expiresAt = expiresAt;
        this.lastUsedAt = now;
    }

    /**
     * 같은 기기로 다시 로그인할 때 기기 정보를 덮어쓴다. {@link #rotate}와 함께 호출한다.
     */
    public void changeDevice(ClientType clientType, String deviceName) {
        this.clientType = clientType;
        this.deviceName = deviceName;
    }

    public boolean matches(String refreshTokenHash) {
        return this.refreshTokenHash.equals(refreshTokenHash);
    }

    public boolean isExpired(Instant now) {
        return !expiresAt.isAfter(now);
    }

}
