package com.chuseok22.jjalpick.auth.application.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @NotBlank String idToken,
    @NotBlank String deviceIdentifier,
    @NotBlank String deviceName
) {

}
