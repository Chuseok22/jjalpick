package com.chuseok22.mimefit.auth.application.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * refreshToken은 DESKTOP만 본문으로 보낸다. WEBSITE는 쿠키로 받는다.
 */
public record RefreshRequest(
    @NotBlank String deviceIdentifier,
    String refreshToken
) {

}
