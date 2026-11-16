package com.chuseok22.jjalpick.auth.application.dto.response;

import com.chuseok22.jjalpick.member.core.entity.Member;
import java.time.Instant;
import java.util.UUID;

/**
 * 내 정보 조회 응답.
 */
public record MemberInformationResponse(
    UUID id,
    String email,
    String name,
    Instant createdAt
) {

    public static MemberInformationResponse from(Member member) {
        return new MemberInformationResponse(
            member.getId(), member.getEmail(), member.getName(), member.getCreatedAt());
    }

}
