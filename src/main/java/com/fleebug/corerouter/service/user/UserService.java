package com.fleebug.corerouter.service.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fleebug.corerouter.dto.user.request.LoginRequest;
import com.fleebug.corerouter.dto.user.request.RegisterRequest;
import com.fleebug.corerouter.dto.user.response.AuthResponse;
import com.fleebug.corerouter.enums.user.UserStatus;
import com.fleebug.corerouter.model.user.User;
import com.fleebug.corerouter.repository.user.UserRepository;
import com.fleebug.corerouter.service.token.TokenService;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    /**
     * Register a new user with email and password
     * 
     * @param registerRequest contains username, email, password and confirmPassword
     * @return AuthResponse with user details and success status
     * @throws IllegalArgumentException if email already exists or passwords don't match
     */
    public AuthResponse register(RegisterRequest registerRequest) {
        log.info("Attempting to register user with email: {}", registerRequest.getEmail());

        // Validate passwords match
        if (!registerRequest.getPassword().equals(registerRequest.getConfirmPassword())) {
            log.warn("Password mismatch during registration for email: {}", registerRequest.getEmail());
            throw new IllegalArgumentException("Passwords do not match");
        }

        // Check if email already exists
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            log.warn("Registration failed - email already exists: {}", registerRequest.getEmail());
            throw new IllegalArgumentException("Email already registered");
        }

        // Check if username already exists
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            log.warn("Registration failed - username already exists: {}", registerRequest.getUsername());
            throw new IllegalArgumentException("Username already taken");
        }

        // Hash password using BCrypt
        String hashedPassword = passwordEncoder.encode(registerRequest.getPassword());

        // Create new user
        User user = User.builder()
                .username(registerRequest.getUsername())
                .email(registerRequest.getEmail())
                .password(hashedPassword)
                .profileImage(registerRequest.getProfileImage())
                .emailSubscribed(registerRequest.isEmailSubscribed())
                .status(UserStatus.ACTIVE)
                .build();

        User savedUser = userRepository.save(user);
        log.info("User registered successfully with ID: {}", savedUser.getUserId());

        return tokenService.buildAuthResponse(savedUser);
    }

    /**
     * Login user with email and password
     * 
     * @param loginRequest contains email and password
     * @return AuthResponse with user details and success status
     * @throws IllegalArgumentException if user not found or password is incorrect
     */
    public AuthResponse login(LoginRequest loginRequest) {
        log.info("Login attempt for email: {}", loginRequest.getEmail());

        // Find user by email
        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> {
                    log.warn("Login failed - user not found with email: {}", loginRequest.getEmail());
                    return new IllegalArgumentException("Invalid email or password");
                });

        // Check if user is active
        if (user.getStatus() != UserStatus.ACTIVE) {
            log.warn("Login failed - user account is not active. Email: {}, Status: {}", 
                    loginRequest.getEmail(), user.getStatus());
            throw new IllegalArgumentException("User account is not active");
        }

        // Verify password using BCrypt
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            log.warn("Login failed - invalid password for email: {}", loginRequest.getEmail());
            throw new IllegalArgumentException("Invalid email or password");
        }

        log.info("User logged in successfully. User ID: {}", user.getUserId());

        return tokenService.buildAuthResponse(user);
    }

    /**
     * Get user by email
     * 
     * @param email user email
     * @return User object if found
     * @throws IllegalArgumentException if user not found
     */
    public User getUserByEmail(String email) {
        log.info("Fetching user with email: {}", email);
        return userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("User not found with email: {}", email);
                    return new IllegalArgumentException("User not found");
                });
    }

    /**
     * Get user by ID
     * 
     * @param userId user ID
     * @return User object if found
     * @throws IllegalArgumentException if user not found
     */
    public User getUserById(Integer userId) {
        log.info("Fetching user with ID: {}", userId);
        return userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found with ID: {}", userId);
                    return new IllegalArgumentException("User not found");
                });
    }

    /**
     * Update user password
     * 
     * @param userId user ID
     * @param oldPassword current password
     * @param newPassword new password
     * @return AuthResponse with success status
     * @throws IllegalArgumentException if user not found or old password is incorrect
     */
    public AuthResponse changePassword(Integer userId, String oldPassword, String newPassword) {
        log.info("Attempting to change password for user ID: {}", userId);

        User user = getUserById(userId);

        // Verify old password
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            log.warn("Change password failed - invalid old password for user ID: {}", userId);
            throw new IllegalArgumentException("Invalid old password");
        }

        // Hash and update new password
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("Password changed successfully for user ID: {}", userId);

        return tokenService.buildAuthResponse(user);
    }

  }