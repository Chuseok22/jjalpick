package com.chuseok22.mimefit.auth.core.repository;

import com.chuseok22.mimefit.auth.core.entity.LoginSession;
import com.chuseok22.mimefit.member.core.entity.Member;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * LoginSession은 행을 삭제하는 예외 엔티티이므로 조회 메서드에 deleted 필터를 두지 않는다.
 */
public interface LoginSessionRepository extends JpaRepository<LoginSession, UUID> {

    Optional<LoginSession> findByMemberAndDeviceIdentifier(Member member, String deviceIdentifier);

    Optional<LoginSession> findByRefreshTokenHash(String refreshTokenHash);

    void deleteByMemberAndExpiresAtLessThanEqual(Member member, Instant now);

}
