package com.chuseok22.jjalpick.admin.infrastructure.config;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.chuseok22.jjalpick.admin.application.controller.AdminDashboardController;
import com.chuseok22.jjalpick.admin.application.controller.AdminLoginController;
import com.chuseok22.jjalpick.auth.core.port.TokenProvider;
import com.chuseok22.jjalpick.auth.infrastructure.config.JwtAccessDeniedHandler;
import com.chuseok22.jjalpick.auth.infrastructure.config.JwtAuthenticationEntryPoint;
import com.chuseok22.jjalpick.auth.infrastructure.config.SecurityConfig;
import com.chuseok22.jjalpick.member.core.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {AdminLoginController.class, AdminDashboardController.class})
@Import({AdminSecurityConfig.class, SecurityConfig.class, JwtAuthenticationEntryPoint.class,
    JwtAccessDeniedHandler.class, PasswordEncoderConfig.class})
class AdminSecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private TokenProvider tokenProvider;

    @MockitoBean
    private MemberRepository memberRepository;

    @Test
    @DisplayName("미인증 /admin 요청은 로그인 페이지로 리다이렉트한다")
    void unauthenticatedAdmin_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/admin"))
            .andExpect(status().isFound())
            .andExpect(redirectedUrl("/admin/login"));
    }

    @Test
    @DisplayName("로그인 페이지는 공개다")
    void loginPage_isPublic() throws Exception {
        mockMvc.perform(get("/admin/login"))
            .andExpect(status().isOk())
            .andExpect(view().name("admin/login"));
    }

    @Test
    @DisplayName("CSRF 없는 로그아웃은 403이다")
    void logoutWithoutCsrf_isForbidden() throws Exception {
        mockMvc.perform(post("/admin/logout").with(user("admin").roles("ADMIN")))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CSRF 포함 로그아웃은 로그인 페이지로 이동한다")
    void logoutWithCsrf_redirectsToLoginWithLogoutParam() throws Exception {
        mockMvc.perform(post("/admin/logout").with(user("admin").roles("ADMIN")).with(csrf()))
            .andExpect(status().isFound())
            .andExpect(redirectedUrl("/admin/login?logout"));
    }

    @Test
    @DisplayName("틀린 비밀번호는 오류 로그인 페이지로 이동한다")
    void wrongPassword_redirectsToLoginError() throws Exception {
        given(userDetailsService.loadUserByUsername("admin")).willReturn(
            User.withUsername("admin").password(passwordEncoder.encode("correct-password")).roles("ADMIN").build());

        mockMvc.perform(post("/admin/login").param("username", "admin").param("password", "wrong-password")
                .with(csrf()))
            .andExpect(status().isFound())
            .andExpect(redirectedUrl("/admin/login?error"));
    }

    @Test
    @DisplayName("없는 아이디도 같은 오류 응답이다")
    void unknownUser_redirectsToLoginError() throws Exception {
        given(userDetailsService.loadUserByUsername("ghost")).willThrow(new UsernameNotFoundException("ghost"));

        mockMvc.perform(post("/admin/login").param("username", "ghost").param("password", "any-password")
                .with(csrf()))
            .andExpect(status().isFound())
            .andExpect(redirectedUrl("/admin/login?error"));
    }

    @Test
    @DisplayName("API는 토큰이 없으면 리다이렉트가 아니라 401이다")
    void apiWithoutToken_stays401NotLoginRedirect() throws Exception {
        mockMvc.perform(get("/api/members/me"))
            .andExpect(status().isUnauthorized())
            .andExpect(header().doesNotExist("Location"));
    }

    @Test
    @DisplayName("관리자 정적 자산과 WebJar는 공개이고 200으로 응답한다")
    void adminAssets_arePublic() throws Exception {
        mockMvc.perform(get("/admin/assets/admin.css")).andExpect(status().isOk());
        mockMvc.perform(get("/webjars/daisyui/dist/full.css")).andExpect(status().isOk());
        mockMvc.perform(get("/webjars/htmx.org/dist/htmx.min.js")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("올바른 자격 증명으로 로그인하면 /admin으로 이동하고 ADMIN으로 인증된다")
    void login_correctCredentials_redirectsToAdminAndAuthenticatesAsAdmin() throws Exception {
        given(userDetailsService.loadUserByUsername("admin")).willReturn(
            User.withUsername("admin").password(passwordEncoder.encode("correct-password")).roles("ADMIN").build());

        mockMvc.perform(post("/admin/login").param("username", "admin").param("password", "correct-password")
                .with(csrf()))
            .andExpect(status().isFound())
            .andExpect(redirectedUrl("/admin"))
            .andExpect(authenticated().withRoles("ADMIN"));
    }

    @Test
    @DisplayName("CSRF 없는 로그인은 403이다")
    void login_withoutCsrf_isForbidden() throws Exception {
        mockMvc.perform(post("/admin/login").param("username", "admin").param("password", "correct-password"))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ADMIN이 아닌 역할은 /admin에 접근할 수 없다")
    void adminPageWithNonAdminRole_isForbidden() throws Exception {
        mockMvc.perform(get("/admin").with(user("member").roles("USER")))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ROLE_ADMIN 인증이면 대시보드에 아이디를 보여 준다")
    void dashboard_showsUsernameAndUsesAdminRole() throws Exception {
        mockMvc.perform(get("/admin").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(view().name("admin/dashboard"))
            .andExpect(model().attribute("adminUsername", "admin"));
    }

}
