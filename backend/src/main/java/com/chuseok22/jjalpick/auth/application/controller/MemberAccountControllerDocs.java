package com.chuseok22.jjalpick.auth.application.controller;

import com.chuseok22.jjalpick.auth.application.dto.response.MemberInformationResponse;
import com.chuseok22.jjalpick.auth.core.support.CustomUserDetails;
import com.chuseok22.jjalpick.common.application.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "회원", description = "내 정보 조회, 탈퇴")
public interface MemberAccountControllerDocs {

    @Operation(summary = "내 정보 조회",
        description = "액세스 토큰의 회원 정보를 반환한다. `createdAt`은 KST 오프셋이 붙은 ISO-8601 문자열이다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "401",
            description = "`UNAUTHENTICATED`: 액세스 토큰이 없거나 올바르지 않거나 탈퇴한 회원임. "
                + "`ACCESS_TOKEN_EXPIRED`: 액세스 토큰이 만료됨. 토큰을 갱신해야 한다",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<MemberInformationResponse> getMyInformation(
        @Parameter(hidden = true) CustomUserDetails principal);

    @Operation(summary = "탈퇴",
        description = """
            회원을 탈퇴 처리(soft delete)하고 보관 중인 리프레시 토큰을 삭제한다.
            같은 Google 계정으로 다시 로그인하면 새 회원으로 가입한다.
            이미 발급된 액세스 토큰은 탈퇴한 회원이면 거부된다.""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "탈퇴 완료(본문 없음)"),
        @ApiResponse(responseCode = "401",
            description = "`UNAUTHENTICATED`: 액세스 토큰이 없거나 올바르지 않거나 이미 탈퇴한 회원임. "
                + "`ACCESS_TOKEN_EXPIRED`: 액세스 토큰이 만료됨. 토큰을 갱신해야 한다",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<Void> withdraw(@Parameter(hidden = true) CustomUserDetails principal);

}
