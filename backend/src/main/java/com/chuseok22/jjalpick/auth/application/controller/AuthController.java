package com.chuseok22.jjalpick.auth.application.controller;

import com.chuseok22.logging.annotation.LogMonitoring;
import com.chuseok22.jjalpick.auth.application.dto.request.LoginRequest;
import com.chuseok22.jjalpick.auth.application.dto.request.LogoutRequest;
import com.chuseok22.jjalpick.auth.application.dto.request.RefreshRequest;
import com.chuseok22.jjalpick.auth.application.dto.response.TokenResponse;
import com.chuseok22.jjalpick.auth.application.service.AuthService;
import com.chuseok22.jjalpick.auth.application.service.AuthTokens;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController implements AuthControllerDocs {

    private final AuthService authService;

    @Override
    @LogMonitoring
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return toTokenResponse(authService.login(request));
    }

    @Override
    @LogMonitoring
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return toTokenResponse(authService.refresh(request.refreshToken(), request.deviceIdentifier()));
    }

    @Override
    @LogMonitoring
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<TokenResponse> toTokenResponse(AuthTokens tokens) {
        return ResponseEntity.ok(
            new TokenResponse(tokens.accessToken(), tokens.accessTokenExpiresAt(), tokens.refreshToken()));
    }

}
