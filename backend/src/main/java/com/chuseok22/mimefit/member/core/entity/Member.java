package com.chuseok22.mimefit.member.core.entity;

import com.chuseok22.mimefit.common.core.entity.BaseEntity;
import com.chuseok22.mimefit.common.core.exception.CustomException;
import com.chuseok22.mimefit.common.core.exception.ErrorCode;
import com.chuseok22.mimefit.member.core.constant.MemberStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
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

}
