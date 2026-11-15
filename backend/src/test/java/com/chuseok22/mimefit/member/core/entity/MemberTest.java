package com.chuseok22.mimefit.member.core.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.chuseok22.mimefit.common.core.exception.CustomException;
import com.chuseok22.mimefit.common.core.exception.ErrorCode;
import com.chuseok22.mimefit.member.core.constant.MemberStatus;
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

}
