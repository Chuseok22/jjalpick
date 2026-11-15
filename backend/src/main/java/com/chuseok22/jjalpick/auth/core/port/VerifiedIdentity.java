package com.chuseok22.jjalpick.auth.core.port;

/**
 * 검증이 끝난 신원 정보. name은 제공자가 주지 않으면 null이다.
 */
public record VerifiedIdentity(String firebaseUid, String email, String name) {

}
