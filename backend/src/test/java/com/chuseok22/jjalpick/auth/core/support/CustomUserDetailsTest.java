package com.chuseok22.jjalpick.auth.core.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.chuseok22.jjalpick.member.core.entity.Member;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CustomUserDetailsTest {

    private static final UUID MEMBER_ID = UUID.fromString("0198a1b2-c3d4-7e5f-8a9b-0c1d2e3f4a5b");

    @Test
    void getUsername_returnsMemberId() {
        Member member = mock(Member.class);
        given(member.getId()).willReturn(MEMBER_ID);

        assertThat(new CustomUserDetails(member).getUsername()).isEqualTo(MEMBER_ID.toString());
    }

    @Test
    void getMember_returnsGivenMember() {
        Member member = mock(Member.class);

        assertThat(new CustomUserDetails(member).getMember()).isSameAs(member);
    }

}
