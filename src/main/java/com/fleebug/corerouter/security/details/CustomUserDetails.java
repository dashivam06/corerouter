package com.fleebug.corerouter.security.details;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.fleebug.corerouter.model.user.User;

import java.util.Collection;
import java.util.Collections;

/**
 * Custom UserDetails implementation for Spring Security
 */
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetails implements UserDetails {

    private final User user;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Map user role to GrantedAuthority
        String role = "ROLE_" + user.getRole().name();
        return Collections.singleton(new SimpleGrantedAuthority(role));
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.getStatus().toString().equals("ACTIVE");
    }

    /**
     * Get the underlying User entity
     */
    public User getUser() {
        return user;
    }

    /**
     * Get user ID
     */
    public Integer getUserId() {
        return user.getUserId();
    }
}
