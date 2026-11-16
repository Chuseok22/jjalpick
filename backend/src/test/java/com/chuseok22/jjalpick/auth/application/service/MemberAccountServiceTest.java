package com.chuseok22.jjalpick.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.chuseok22.jjalpick.auth.application.dto.response.MemberInformationResponse;
import com.chuseok22.jjalpick.common.core.exception.CustomException;
import com.chuseok22.jjalpick.common.core.exception.ErrorCode;
import com.chuseok22.jjalpick.member.core.entity.Member;
import com.chuseok22.jjalpick.member.core.repository.MemberRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

class MemberAccountServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-16T00:00:00Z");
    private static final Instant CREATED_AT = Instant.parse("2026-11-01T00:00:00Z");

    private MemberRepository memberRepository;
    private MemberAccountService memberAccountService;
    private Member member;
    private UUID memberId;

    @BeforeEach
    void setUp() {
        memberRepository = Mockito.mock(MemberRepository.class);
        memberAccountService = new MemberAccountService(memberRepository, Clock.fixed(NOW, ZoneOffset.UTC));
        memberId = UUID.randomUUID();
        member = Member.create("firebase-uid", "member@example.com", "홍길동");
        ReflectionTestUtils.setField(member, "id", memberId);
        ReflectionTestUtils.setField(member, "createdAt", CREATED_AT);
    }

    @Test
    void getInformation_returnsMemberFields() {
        MemberInformationResponse response = memberAccountService.getInformation(member);

        assertThat(response.id()).isEqualTo(memberId);
        assertThat(response.email()).isEqualTo("member@example.com");
        assertThat(response.name()).isEqualTo("홍길동");
        assertThat(response.createdAt()).isEqualTo(CREATED_AT);
    }

    @Test
    void withdraw_activeMember_clearsRefreshTokenAndSoftDeletes() {
        member.startRefreshToken("hash", NOW.plusSeconds(60));
        given(memberRepository.findByIdAndDeletedFalse(memberId)).willReturn(Optional.of(member));

        memberAccountService.withdraw(member);

        assertThat(member.isDeleted()).isTrue();
        assertThat(member.getDeletedAt()).isEqualTo(NOW);
        assertThat(member.getRefreshTokenHash()).isNull();
        assertThat(member.getRefreshTokenExpiresAt()).isNull();
    }

    @Test
    void withdraw_alreadyWithdrawn_throwsUnauthenticated() {
        given(memberRepository.findByIdAndDeletedFalse(memberId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> memberAccountService.withdraw(member))
            .isInstanceOfSatisfying(CustomException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.UNAUTHENTICATED));
    }

}
