package com.chuseok22.jjalpick.auth.core.support;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * 리프레시 토큰 원문 대신 DB에 저장할 SHA-256 해시를 만든다.
 */
public final class RefreshTokenHasher {

    private static final String ALGORITHM = "SHA-256";

    private RefreshTokenHasher() {
    }

    /**
     * 소문자 16진수 64자 해시를 반환한다.
     */
    public static String hash(String refreshToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            return HexFormat.of().formatHex(digest.digest(refreshToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(ALGORITHM + " 알고리즘을 사용할 수 없습니다.", exception);
        }
    }

}
