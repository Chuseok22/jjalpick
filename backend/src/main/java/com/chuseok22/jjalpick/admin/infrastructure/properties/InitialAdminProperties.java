package com.chuseok22.jjalpick.admin.infrastructure.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 초기 관리자 계정 설정. 둘 다 선택 값이며 비어 있으면 초기 관리자를 만들지 않는다.
 *
 * @param username 초기 관리자 아이디
 * @param password 초기 관리자 비밀번호(평문)
 */
@ConfigurationProperties(prefix = "admin.initial-account")
public record InitialAdminProperties(String username, String password) {

    // 로그에 비밀번호가 노출되지 않도록 마스킹한다
    @Override
    public String toString() {
        return "InitialAdminProperties[username=" + username + ", password=****]";
    }

}
