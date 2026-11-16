package com.chuseok22.jjalpick.auth.application.controller;

import com.chuseok22.jjalpick.auth.application.dto.request.LoginRequest;
import com.chuseok22.jjalpick.auth.application.dto.request.LogoutRequest;
import com.chuseok22.jjalpick.auth.application.dto.request.RefreshRequest;
import com.chuseok22.jjalpick.auth.application.dto.response.TokenResponse;
import com.chuseok22.jjalpick.common.application.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;

@Tag(name = "인증", description = "Google 로그인, 토큰 갱신, 로그아웃")
public interface AuthControllerDocs {

    @Operation(summary = "로그인",
        description = """
            Firebase ID 토큰(Google 로그인)을 검증해 로그인한다. 활성 회원이 없으면 가입까지 처리한다.
            회원당 로그인은 하나만 유지하며, 다시 로그인하면 이전 로그인의 리프레시 토큰은 더 이상 쓸 수 없다.
            `accessTokenExpiresAt`은 KST 오프셋이 붙은 ISO-8601 문자열이다.""")
    @SecurityRequirements
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "로그인 성공"),
        @ApiResponse(responseCode = "400",
            description = "`INVALID_REQUEST`: `idToken`이 비어 있음(`errorMessage`에 사유). "
                + "`MALFORMED_REQUEST_BODY`: 본문이 올바른 JSON이 아님",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401",
            description = "`INVALID_IDENTITY_TOKEN`: Firebase ID 토큰이 위조·만료되었거나 이메일이 없음",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403",
            description = "`MEMBER_SUSPENDED`: 이용이 정지된 회원",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "503",
            description = "`IDENTITY_PROVIDER_UNAVAILABLE`: Firebase 신원 확인 서비스 장애. 잠시 후 다시 시도한다",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<TokenResponse> login(@Valid LoginRequest request);

    @Operation(summary = "토큰 갱신",
        description = """
            리프레시 토큰으로 새 액세스 토큰과 새 리프레시 토큰을 발급한다(회전). 이전 리프레시 토큰은 다시 쓸 수 없다.
            이미 교체된 리프레시 토큰을 다시 보내면 재사용으로 보고 보관 중인 리프레시 토큰을 삭제한다.
            응답 형식은 로그인과 같다.""")
    @SecurityRequirements
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "갱신 성공"),
        @ApiResponse(responseCode = "400",
            description = "`INVALID_REQUEST`: `refreshToken`이 비어 있음. "
                + "`MALFORMED_REQUEST_BODY`: 본문이 올바른 JSON이 아님",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401",
            description = "`INVALID_REFRESH_TOKEN`: 리프레시 토큰이 위조·만료되었거나 액세스 토큰을 보냈거나, "
                + "이미 교체된 토큰(재사용)이거나, 보관 중인 리프레시 토큰이 없거나, 탈퇴한 회원의 토큰임. 다시 로그인해야 한다",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403",
            description = "`MEMBER_SUSPENDED`: 이용이 정지된 회원",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<TokenResponse> refresh(@Valid RefreshRequest request);

    @Operation(summary = "로그아웃",
        description = """
            본문 `refreshToken`(필수)으로 보관 중인 리프레시 토큰을 삭제한다.
            알 수 없는 토큰이어도 오류 없이 204로 응답한다(멱등).
            이미 발급된 액세스 토큰은 만료될 때까지 유효하므로 클라이언트가 직접 폐기한다.""")
    @SecurityRequirements
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "로그아웃 완료(본문 없음)"),
        @ApiResponse(responseCode = "400",
            description = "`INVALID_REQUEST`: `refreshToken`이 비어 있음. "
                + "`MALFORMED_REQUEST_BODY`: 본문이 없거나 올바른 JSON이 아님",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<Void> logout(@Valid LogoutRequest request);

}
