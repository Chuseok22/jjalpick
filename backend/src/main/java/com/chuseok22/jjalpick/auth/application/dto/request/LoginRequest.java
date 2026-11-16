package com.chuseok22.jjalpick.auth.application.dto.request;

import com.chuseok22.jjalpick.auth.core.constant.ClientType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LoginRequest(
    @NotBlank String idToken,
    @NotNull ClientType clientType,
    @NotBlank String deviceIdentifier,
    @NotBlank String deviceName
) {

}
