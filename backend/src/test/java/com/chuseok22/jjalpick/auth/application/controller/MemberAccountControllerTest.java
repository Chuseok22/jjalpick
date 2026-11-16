package com.chuseok22.jjalpick.auth.application.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chuseok22.jjalpick.auth.application.dto.response.MemberInformationResponse;
import com.chuseok22.jjalpick.auth.application.service.MemberAccountService;
import com.chuseok22.jjalpick.auth.core.support.CustomUserDetails;
import com.chuseok22.jjalpick.common.application.exception.GlobalExceptionHandler;
import com.chuseok22.jjalpick.member.core.entity.Member;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class MemberAccountControllerTest {

    private MemberAccountService memberAccountService;
    private MockMvc mockMvc;
    private Member member;
    private UUID memberId;

    @BeforeEach
    void setUp() {
        memberAccountService = mock(MemberAccountService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new MemberAccountController(memberAccountService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
            .build();

        memberId = UUID.randomUUID();
        member = Member.create("firebase-uid", "member@example.com", "홍길동");
        ReflectionTestUtils.setField(member, "id", memberId);
        CustomUserDetails principal = new CustomUserDetails(member);
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getMyInformation_returns200WithFields() throws Exception {
        given(memberAccountService.getInformation(member)).willReturn(new MemberInformationResponse(
            memberId, "member@example.com", "홍길동", Instant.parse("2026-11-01T00:00:00Z")));

        mockMvc.perform(get("/api/members/me"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(memberId.toString()))
            .andExpect(jsonPath("$.email").value("member@example.com"))
            .andExpect(jsonPath("$.name").value("홍길동"))
            .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void withdraw_returns204AndCallsService() throws Exception {
        mockMvc.perform(delete("/api/members/me"))
            .andExpect(status().isNoContent());

        verify(memberAccountService).withdraw(member);
    }

}
