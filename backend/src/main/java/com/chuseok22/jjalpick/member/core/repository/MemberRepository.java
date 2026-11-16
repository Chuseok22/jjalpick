package com.chuseok22.jjalpick.member.core.repository;

import com.chuseok22.jjalpick.member.core.entity.Member;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, UUID> {

    Optional<Member> findByFirebaseUidAndDeletedFalse(String firebaseUid);

    Optional<Member> findByIdAndDeletedFalse(UUID id);

}
