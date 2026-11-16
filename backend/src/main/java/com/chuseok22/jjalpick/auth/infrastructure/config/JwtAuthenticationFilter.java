package com.chuseok22.jjalpick.auth.infrastructure.config;

import com.chuseok22.jjalpick.auth.core.port.TokenProvider;
import com.chuseok22.jjalpick.auth.core.support.CustomUserDetails;
import com.chuseok22.jjalpick.common.core.exception.CustomException;
import com.chuseok22.jjalpick.common.core.exception.ErrorCode;
import com.chuseok22.jjalpick.member.core.repository.MemberRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Bearer 액세스 토큰을 검증해 인증 객체(principal={@link CustomUserDetails}, 권한 없음)를 설정한다.
 * <p>
 * 토큰의 회원이 없으면(탈퇴 등) 인증하지 않고 UNAUTHENTICATED를 남긴다.
 * 검증에 실패해도 응답하지 않고 에러 코드를 요청 속성에 남긴 채 체인을 계속한다(공개 경로는 토큰이 잘못되어도
 * 통과해야 하므로). 401 응답은 {@link JwtAuthenticationEntryPoint}가 이 속성을 읽어 쓴다.
 * 빈으로 등록하지 않고 {@link SecurityConfig}에서 만들어 서블릿 컨테이너에 이중 등록되지 않게 한다.
 */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String ERROR_CODE_ATTRIBUTE = JwtAuthenticationFilter.class.getName() + ".ERROR_CODE";

    private static final String BEARER_PREFIX = "Bearer ";

    private final TokenProvider tokenProvider;
    private final MemberRepository memberRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
        FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null
            && authorization.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            authenticate(request, authorization.substring(BEARER_PREFIX.length()).trim());
        }
        filterChain.doFilter(request, response);
    }

    private void authenticate(HttpServletRequest request, String token) {
        try {
            UUID memberId = tokenProvider.parseAccessToken(token);
            memberRepository.findByIdAndDeletedFalse(memberId).ifPresentOrElse(
                member -> SecurityContextHolder.getContext().setAuthentication(
                    UsernamePasswordAuthenticationToken.authenticated(new CustomUserDetails(member), null, List.of())),
                () -> request.setAttribute(ERROR_CODE_ATTRIBUTE, ErrorCode.UNAUTHENTICATED));
        } catch (CustomException exception) {
            request.setAttribute(ERROR_CODE_ATTRIBUTE, exception.getErrorCode());
        }
    }

}
