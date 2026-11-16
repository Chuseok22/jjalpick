package com.chuseok22.jjalpick.admin.infrastructure.config;

import com.chuseok22.jjalpick.admin.core.entity.Admin;
import com.chuseok22.jjalpick.admin.core.repository.AdminRepository;
import com.chuseok22.jjalpick.admin.infrastructure.properties.InitialAdminProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 기동 시 활성 관리자가 없고 설정에 아이디·비밀번호가 있으면 초기 관리자를 만든다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InitialAdminRunner implements ApplicationRunner {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final InitialAdminProperties initialAdminProperties;

    @Override
    @Transactional
    public void run(ApplicationArguments arguments) {
        String username = initialAdminProperties.username();
        String password = initialAdminProperties.password();
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return;
        }
        if (adminRepository.existsByDeletedFalse()) {
            return;
        }
        adminRepository.save(Admin.create(username, passwordEncoder.encode(password)));
        log.info("초기 관리자 계정을 생성했습니다");
    }

}
