package com.chuseok22.jjalpick.admin.core.repository;

import com.chuseok22.jjalpick.admin.core.entity.Admin;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminRepository extends JpaRepository<Admin, UUID> {

    Optional<Admin> findByUsernameAndDeletedFalse(String username);

    boolean existsByDeletedFalse();

}
