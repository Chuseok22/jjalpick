package com.chuseok22.jjalpick.auth.application.controller;

import com.chuseok22.logging.annotation.LogMonitoring;
import com.chuseok22.jjalpick.auth.application.dto.request.LoginRequest;
import com.chuseok22.jjalpick.auth.application.dto.request.LogoutRequest;
import com.chuseok22.jjalpick.auth.application.dto.request.RefreshRequest;
import com.chuseok22.jjalpick.auth.application.dto.response.TokenResponse;
import com.chuseok22.jjalpick.auth.application.service.AuthService;
import com.chuseok22.jjalpick.auth.application.service.AuthTokens;
import com.chuseok22.jjalpick.auth.application.support.RefreshTokenCookie;
import com.chuseok22.jjalpick.auth.core.constant.ClientType;
import jakarta.validation.Valid;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController implements AuthControllerDocs {

    private final AuthService authService;
    private final Clock clock;

    @Override
    @LogMonitoring
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return toTokenResponse(authService.login(request));
    }

    @Override
    @LogMonitoring
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(
        @Valid @RequestBody RefreshRequest request,
        @CookieValue(name = RefreshTokenCookie.NAME, required = false) String cookieRefreshToken
    ) {
        String refreshToken = selectRefreshToken(request.refreshToken(), cookieRefreshToken);
        return toTokenResponse(authService.refresh(refreshToken, request.deviceIdentifier()));
    }

    @Override
    @LogMonitoring
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
        @RequestBody(required = false) LogoutRequest request,
        @CookieValue(name = RefreshTokenCookie.NAME, required = false) String cookieRefreshToken
    ) {
        String bodyRefreshToken = request == null ? null : request.refreshToken();
        authService.logout(selectRefreshToken(bodyRefreshToken, cookieRefreshToken));
        ResponseEntity.HeadersBuilder<?> response = ResponseEntity.noContent();
        if (cookieRefreshToken != null) {
            response.header(HttpHeaders.SET_COOKIE, RefreshTokenCookie.expire().toString());
        }
        return response.build();
    }

    // 본문 값(DESKTOP)이 있으면 쓰고, 없으면 쿠키 값(WEBSITE)을 쓴다
    private String selectRefreshToken(String bodyRefreshToken, String cookieRefreshToken) {
        if (bodyRefreshToken != null && !bodyRefreshToken.isBlank()) {
            return bodyRefreshToken;
        }
        return cookieRefreshToken;
    }

    // WEBSITE는 리프레시 토큰을 본문에서 빼고 쿠키로만 내려 주고, DESKTOP은 본문에 담는다
    private ResponseEntity<TokenResponse> toTokenResponse(AuthTokens tokens) {
        if (tokens.clientType() == ClientType.WEBSITE) {
            String cookie = RefreshTokenCookie.create(
                tokens.refreshToken(), tokens.refreshTokenExpiresAt(), clock.instant()).toString();
            return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie)
                .body(new TokenResponse(tokens.accessToken(), tokens.accessTokenExpiresAt(), null));
        }
        return ResponseEntity.ok(
            new TokenResponse(tokens.accessToken(), tokens.accessTokenExpiresAt(), tokens.refreshToken()));
    }

}
