package com.chuseok22.jjalpick.auth.infrastructure.config;

import com.chuseok22.jjalpick.common.application.exception.ErrorResponse;
import com.chuseok22.jjalpick.common.core.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * 인증되지 않은 요청에 401 {@link ErrorResponse}를 쓴다. 필터가 남긴 에러 코드가 있으면 그 코드를,
 * 없으면 {@link ErrorCode#UNAUTHENTICATED}를 쓴다.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
        AuthenticationException authenticationException) throws IOException {
        ErrorCode errorCode = request.getAttribute(JwtAuthenticationFilter.ERROR_CODE_ATTRIBUTE)
            instanceof ErrorCode markedErrorCode ? markedErrorCode : ErrorCode.UNAUTHENTICATED;
        response.setStatus(errorCode.httpStatus());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(ErrorResponse.of(errorCode)));
    }

}
