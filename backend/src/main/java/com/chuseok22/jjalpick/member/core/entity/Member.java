package com.chuseok22.jjalpick.member.core.entity;

import com.chuseok22.jjalpick.common.core.entity.BaseEntity;
import com.chuseok22.jjalpick.common.core.exception.CustomException;
import com.chuseok22.jjalpick.common.core.exception.ErrorCode;
import com.chuseok22.jjalpick.member.core.constant.MemberStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "member")
public class Member extends BaseEntity {

    // Firebase uid. 로그인 제공자와 무관한 회원 조회 키
    @Column(nullable = false)
    private String firebaseUid;

    @Column(nullable = false)
    private String email;

    @Column
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberStatus status;

    // 회원당 하나만 보관하는 리프레시 토큰의 해시. 로그인하지 않은 상태면 null
    @Column
    private String refreshTokenHash;

    @Column
    private Instant refreshTokenExpiresAt;

    /**
     * 최초 로그인 시 활성 상태의 회원을 만든다.
     */
    public static Member create(String firebaseUid, String email, String name) {
        Member member = new Member();
        member.firebaseUid = firebaseUid;
        member.email = email;
        member.name = name;
        member.status = MemberStatus.ACTIVE;
        return member;
    }

    /**
     * 정지된 회원이면 로그인을 거부한다.
     */
    public void assertCanLogin() {
        if (status == MemberStatus.SUSPENDED) {
            throw new CustomException(ErrorCode.MEMBER_SUSPENDED);
        }
    }

    /**
     * 리프레시 토큰을 새로 보관한다. 이전 토큰은 덮어써서 무효가 된다.
     */
    public void startRefreshToken(String refreshTokenHash, Instant expiresAt) {
        this.refreshTokenHash = refreshTokenHash;
        this.refreshTokenExpiresAt = expiresAt;
    }

    /**
     * 보관 중인 리프레시 토큰을 지운다.
     */
    public void clearRefreshToken() {
        this.refreshTokenHash = null;
        this.refreshTokenExpiresAt = null;
    }

    /**
     * 보관 중인 리프레시 토큰 해시와 일치하는지 확인한다.
     */
    public boolean matchesRefreshToken(String refreshTokenHash) {
        return this.refreshTokenHash != null && this.refreshTokenHash.equals(refreshTokenHash);
    }

    /**
     * 리프레시 토큰이 없거나 만료 시각에 도달했으면 true.
     */
    public boolean isRefreshTokenExpired(Instant now) {
        return refreshTokenExpiresAt == null || !now.isBefore(refreshTokenExpiresAt);
    }

}
