package com.chuseok22.jjalpick.admin.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.chuseok22.jjalpick.admin.core.entity.Admin;
import com.chuseok22.jjalpick.admin.core.repository.AdminRepository;
import com.chuseok22.jjalpick.admin.infrastructure.properties.InitialAdminProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

class InitialAdminRunnerTest {

    private static final String USERNAME = "initial-admin";
    private static final String PASSWORD = "plain-secret-password";

    private AdminRepository adminRepository;
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        adminRepository = mock(AdminRepository.class);
        passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    private InitialAdminRunner runner(String username, String password) {
        return new InitialAdminRunner(adminRepository, passwordEncoder, new InitialAdminProperties(username, password));
    }

    @Test
    void run_noAdminAndBothPresent_savesAdminWithEncodedPassword() {
        given(adminRepository.existsByDeletedFalse()).willReturn(false);

        runner(USERNAME, PASSWORD).run(null);

        ArgumentCaptor<Admin> captor = ArgumentCaptor.forClass(Admin.class);
        verify(adminRepository).save(captor.capture());
        Admin saved = captor.getValue();
        assertThat(saved.getUsername()).isEqualTo(USERNAME);
        assertThat(saved.getPasswordHash()).isNotEqualTo(PASSWORD);
        assertThat(passwordEncoder.matches(PASSWORD, saved.getPasswordHash())).isTrue();
    }

    @Test
    void run_adminExists_doesNothing() {
        given(adminRepository.existsByDeletedFalse()).willReturn(true);

        runner(USERNAME, PASSWORD).run(null);

        verify(adminRepository, never()).save(any());
    }

    @Test
    void run_blankUsername_doesNothing() {
        runner(" ", PASSWORD).run(null);

        verifyNoInteractions(adminRepository);
    }

    @Test
    void run_blankPassword_doesNothing() {
        runner(USERNAME, null).run(null);

        verifyNoInteractions(adminRepository);
    }

    @Test
    void properties_toString_masksPassword() {
        assertThat(new InitialAdminProperties(USERNAME, PASSWORD).toString()).doesNotContain(PASSWORD);
    }

}
