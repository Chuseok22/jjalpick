package com.chuseok22.jjalpick.auth.application.service;

import com.chuseok22.jjalpick.auth.application.dto.response.MemberInformationResponse;
import com.chuseok22.jjalpick.common.core.exception.CustomException;
import com.chuseok22.jjalpick.common.core.exception.ErrorCode;
import com.chuseok22.jjalpick.member.core.entity.Member;
import com.chuseok22.jjalpick.member.core.repository.MemberRepository;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 인증된 회원 본인의 정보 조회와 탈퇴를 담당한다.
 */
@Service
@RequiredArgsConstructor
public class MemberAccountService {

    private final MemberRepository memberRepository;
    private final Clock clock;

    public MemberInformationResponse getInformation(Member member) {
        return MemberInformationResponse.from(member);
    }

    /**
     * 컨트롤러가 넘긴 회원은 분리 상태이므로 id로 다시 조회해 리프레시 토큰을 비우고 soft delete 한다.
     */
    @Transactional
    public void withdraw(Member member) {
        Member activeMember = memberRepository.findByIdAndDeletedFalse(member.getId())
            .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHENTICATED));
        activeMember.clearRefreshToken();
        activeMember.delete(clock);
    }

}
