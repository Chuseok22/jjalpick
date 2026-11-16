package com.chuseok22.jjalpick.admin.infrastructure.config;

import com.chuseok22.jjalpick.admin.core.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * 활성 관리자 계정을 {@code ROLE_ADMIN} 사용자로 불러온다.
 */
@Service
@RequiredArgsConstructor
public class AdminUserDetailsService implements UserDetailsService {

    private static final String ADMIN_ROLE = "ADMIN";

    private final AdminRepository adminRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return adminRepository.findByUsernameAndDeletedFalse(username)
            .map(admin -> User.withUsername(username)
                .password(admin.getPasswordHash())
                .roles(ADMIN_ROLE)
                .build())
            .orElseThrow(() -> new UsernameNotFoundException("관리자를 찾을 수 없습니다"));
    }

}
