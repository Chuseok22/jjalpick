package com.chuseok22.jjalpick.auth.infrastructure.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Firebase 설정.
 *
 * @param serviceAccountLocation 서비스 계정 JSON 위치(예: classpath:firebase-service-account.json)
 */
@ConfigurationProperties(prefix = "firebase")
public record FirebaseProperties(String serviceAccountLocation) {

    public FirebaseProperties {
        if (serviceAccountLocation == null || serviceAccountLocation.isBlank()) {
            throw new IllegalStateException("firebase.service-account-location이 비어 있습니다");
        }
    }

}
