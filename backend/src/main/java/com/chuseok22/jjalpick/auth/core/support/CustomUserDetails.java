package com.chuseok22.jjalpick.auth.core.support;

import com.chuseok22.jjalpick.member.core.entity.Member;
import java.util.Collection;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * 인증된 요청의 주체. 인증 시점에 조회한 회원을 담는다(권한 없음, 비밀번호 없음).
 */
@Getter
@RequiredArgsConstructor
public class CustomUserDetails implements UserDetails {

    private final Member member;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return member.getId().toString();
    }

}
