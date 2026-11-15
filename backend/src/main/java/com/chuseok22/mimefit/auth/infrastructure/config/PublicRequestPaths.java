package com.chuseok22.mimefit.auth.infrastructure.config;

/**
 * 인증 없이 여는 경로. 공개 경로는 이 클래스 한 곳에서만 관리한다.
 */
public final class PublicRequestPaths {

    public static final String LOGIN = "/api/auth/login";
    public static final String REFRESH = "/api/auth/refresh";
    public static final String LOGOUT = "/api/auth/logout";
    // Dockerfile과 CICD 헬스체크가 호출하므로 반드시 공개한다
    public static final String HEALTH = "/actuator/health";
    // 필터 체인 밖으로 나온 예외의 ERROR 디스패치도 인가 대상이라, 열지 않으면 원래 오류 대신 401이 된다
    public static final String ERROR = "/error";
    public static final String API_DOCS = "/v3/api-docs/**";
    public static final String SWAGGER_UI = "/swagger-ui/**";

    public static final String[] ALL = {LOGIN, REFRESH, LOGOUT, HEALTH, ERROR, API_DOCS, SWAGGER_UI};

    private PublicRequestPaths() {
    }

}
