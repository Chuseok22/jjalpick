package com.chuseok22.jjalpick.admin.core.entity;

import com.chuseok22.jjalpick.common.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "admin")
public class Admin extends BaseEntity {

    @Column(nullable = false)
    private String username;

    // 해시된 비밀번호. 평문은 저장하지 않는다
    @Column(nullable = false)
    private String passwordHash;

    /**
     * 관리자 계정을 만든다. 비밀번호는 해시된 값을 받는다.
     */
    public static Admin create(String username, String passwordHash) {
        Admin admin = new Admin();
        admin.username = username;
        admin.passwordHash = passwordHash;
        return admin;
    }

}
