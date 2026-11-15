package com.chuseok22.jjalpick.auth.application.dto.request;

/**
 * refreshToken은 DESKTOP만 본문으로 보낸다. WEBSITE는 쿠키로 받는다.
 */
public record LogoutRequest(String refreshToken) {

}
