package com.fleebug.corerouter.security.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.fleebug.corerouter.entity.user.User;
import com.fleebug.corerouter.repository.user.UserRepository;
import com.fleebug.corerouter.security.details.CustomUserDetails;

/**
 * Custom UserDetailsService implementation for Spring Security
 * Loads user details from database based on email
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.info("Loading user details for email: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("User not found with email: {}", email);
                    return new UsernameNotFoundException("User not found with email: " + email);
                });

        log.debug("User details loaded successfully for email: {}", email);
        return new CustomUserDetails(user);
    }
}
