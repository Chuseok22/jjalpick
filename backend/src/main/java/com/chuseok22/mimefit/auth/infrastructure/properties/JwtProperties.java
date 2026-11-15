package com.chuseok22.mimefit.auth.infrastructure.properties;

import java.util.Base64;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 설정. HS512 키는 jjwt가 서명 시점에야 길이를 검사하므로 기동 시 직접 검증한다.
 *
 * @param secretKey Base64로 인코딩한 64바이트 이상의 키
 */
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(String secretKey, long accessExpMillis, long refreshExpMillis, String issuer) {

    private static final int MINIMUM_SECRET_KEY_BYTES = 64;

    public JwtProperties {
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException("jwt.secret-key가 비어 있습니다");
        }
        if (decodeSecretKey(secretKey).length < MINIMUM_SECRET_KEY_BYTES) {
            throw new IllegalStateException("jwt.secret-key는 Base64 디코딩 기준 최소 64바이트여야 합니다");
        }
        if (accessExpMillis <= 0 || refreshExpMillis <= 0) {
            throw new IllegalStateException("jwt 만료 시간은 0보다 커야 합니다");
        }
        if (issuer == null || issuer.isBlank()) {
            throw new IllegalStateException("jwt.issuer가 비어 있습니다");
        }
    }

    private static byte[] decodeSecretKey(String encodedSecretKey) {
        try {
            return Base64.getDecoder().decode(encodedSecretKey);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("jwt.secret-key는 Base64 형식이어야 합니다");
        }
    }

    // 로그에 키가 노출되지 않도록 마스킹한다
    @Override
    public String toString() {
        return "JwtProperties[secretKey=****, accessExpMillis=" + accessExpMillis
            + ", refreshExpMillis=" + refreshExpMillis + ", issuer=" + issuer + "]";
    }

}
