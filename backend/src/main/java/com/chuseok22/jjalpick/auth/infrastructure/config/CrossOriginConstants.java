package com.chuseok22.jjalpick.auth.infrastructure.config;

import java.util.List;

/**
 * CORS 허용 규칙. 허용 출처는 이 클래스 한 곳에서만 관리한다.
 */
public final class CrossOriginConstants {

    // 로컬 Website 개발 서버만 허용한다. 운영 출처는 도메인 확정 후 추가한다
    public static final List<String> ALLOWED_ORIGINS = List.of("http://localhost:5173");
    public static final List<String> ALLOWED_METHODS = List.of("GET", "POST", "OPTIONS");
    public static final List<String> ALLOWED_HEADERS = List.of("Authorization", "Content-Type");
    // WEBSITE의 리프레시 토큰 쿠키 흐름에 필요하다
    public static final boolean ALLOW_CREDENTIALS = true;

    private CrossOriginConstants() {
    }

}
