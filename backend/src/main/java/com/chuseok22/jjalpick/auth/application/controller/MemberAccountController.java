package com.chuseok22.jjalpick.auth.application.controller;

import com.chuseok22.jjalpick.auth.application.dto.response.MemberInformationResponse;
import com.chuseok22.jjalpick.auth.application.service.MemberAccountService;
import com.chuseok22.jjalpick.auth.core.support.CustomUserDetails;
import com.chuseok22.logging.annotation.LogMonitoring;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberAccountController implements MemberAccountControllerDocs {

    private final MemberAccountService memberAccountService;

    @Override
    @LogMonitoring
    @GetMapping("/me")
    public ResponseEntity<MemberInformationResponse> getMyInformation(
        @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(memberAccountService.getInformation(principal.getMember()));
    }

    @Override
    @LogMonitoring
    @DeleteMapping("/me")
    public ResponseEntity<Void> withdraw(@AuthenticationPrincipal CustomUserDetails principal) {
        memberAccountService.withdraw(principal.getMember());
        return ResponseEntity.noContent().build();
    }

}
