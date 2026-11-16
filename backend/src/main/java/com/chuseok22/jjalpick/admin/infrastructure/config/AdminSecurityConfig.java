package com.chuseok22.jjalpick.admin.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 관리자 화면 전용 보안 필터 체인. 세션과 CSRF를 쓰며 API 체인(JWT)보다 먼저 매칭한다.
 */
@Configuration
public class AdminSecurityConfig {

    private static final String LOGIN_PATH = "/admin/login";

    @Bean
    @Order(1)
    public SecurityFilterChain adminSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/admin/**", "/webjars/**")
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers(LOGIN_PATH, "/webjars/**", "/admin/assets/**").permitAll()
                .anyRequest().hasRole("ADMIN"))
            .requestCache(requestCache -> requestCache.disable())
            .formLogin(form -> form
                .loginPage(LOGIN_PATH)
                .loginProcessingUrl(LOGIN_PATH)
                .failureUrl(LOGIN_PATH + "?error")
                .defaultSuccessUrl("/admin", true))
            .logout(logout -> logout
                .logoutUrl("/admin/logout")
                .logoutSuccessUrl(LOGIN_PATH + "?logout"));
        return http.build();
    }

}
