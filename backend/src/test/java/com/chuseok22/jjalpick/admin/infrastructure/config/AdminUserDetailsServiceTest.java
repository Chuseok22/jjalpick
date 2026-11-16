package com.chuseok22.jjalpick.admin.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.chuseok22.jjalpick.admin.core.entity.Admin;
import com.chuseok22.jjalpick.admin.core.repository.AdminRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
class AdminUserDetailsServiceTest {

    @Mock
    private AdminRepository adminRepository;

    @InjectMocks
    private AdminUserDetailsService adminUserDetailsService;

    @Test
    @DisplayName("활성 관리자는 ROLE_ADMIN 권한과 저장된 해시를 가진 사용자로 반환한다")
    void loadUserByUsername_existingAdmin_returnsUserWithRoleAdmin() {
        given(adminRepository.findByUsernameAndDeletedFalse("admin"))
            .willReturn(Optional.of(Admin.create("admin", "{bcrypt}stored-hash")));

        UserDetails userDetails = adminUserDetailsService.loadUserByUsername("admin");

        assertThat(userDetails.getUsername()).isEqualTo("admin");
        assertThat(userDetails.getPassword()).isEqualTo("{bcrypt}stored-hash");
        assertThat(userDetails.getAuthorities()).extracting(GrantedAuthority::getAuthority)
            .containsExactly("ROLE_ADMIN");
    }

    @Test
    @DisplayName("없는 관리자는 UsernameNotFoundException을 던진다")
    void loadUserByUsername_unknown_throwsUsernameNotFound() {
        given(adminRepository.findByUsernameAndDeletedFalse("ghost")).willReturn(Optional.empty());

        assertThatThrownBy(() -> adminUserDetailsService.loadUserByUsername("ghost"))
            .isInstanceOf(UsernameNotFoundException.class);
    }

}
