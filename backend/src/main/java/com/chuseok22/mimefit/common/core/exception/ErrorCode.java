package com.chuseok22.mimefit.common.core.exception;

/**
 * 서비스 전체의 에러 코드. enum 하나로 관리하고, 도메인 코드는 도메인별 주석 섹션으로 추가한다.
 * 클라이언트는 enum 상수 이름({@link #name()})으로 분기한다.
 */
public enum ErrorCode {

    // 공통
    INVALID_REQUEST(400, "잘못된 요청입니다."),
    MALFORMED_REQUEST_BODY(400, "요청 본문을 읽을 수 없습니다."),
    RESOURCE_NOT_FOUND(404, "요청한 리소스를 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED(405, "지원하지 않는 HTTP 메서드입니다."),
    UNSUPPORTED_MEDIA_TYPE(415, "지원하지 않는 미디어 타입입니다."),
    STATE_CONFLICT(409, "현재 상태에서는 처리할 수 없습니다."),

    // 인증
    ACCESS_TOKEN_EXPIRED(401, "액세스 토큰이 만료되었습니다."),
    UNAUTHENTICATED(401, "인증이 필요합니다."),
    INVALID_IDENTITY_TOKEN(401, "신원 확인 토큰이 유효하지 않습니다."),
    INVALID_REFRESH_TOKEN(401, "리프레시 토큰이 유효하지 않습니다."),
    MEMBER_SUSPENDED(403, "이용이 정지된 회원입니다."),
    IDENTITY_PROVIDER_UNAVAILABLE(503, "신원 확인 서비스를 일시적으로 사용할 수 없습니다."),
    ACCESS_DENIED(403, "접근 권한이 없습니다."),

    // 공통(서버)
    INTERNAL_SERVER_ERROR(500, "서버 내부 오류가 발생했습니다.");

    private final int httpStatus;
    private final String message;

    ErrorCode(int httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }

    public int httpStatus() {
        return httpStatus;
    }

    public String message() {
        return message;
    }

}
