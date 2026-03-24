package com.fleebug.corerouter.service.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fleebug.corerouter.dto.otp.FinalRegistrationRequest;
import com.fleebug.corerouter.dto.otp.RequestOtpResponse;
import com.fleebug.corerouter.dto.otp.VerifyOtpResponse;
import com.fleebug.corerouter.dto.user.request.LoginRequest;
import com.fleebug.corerouter.dto.user.response.AuthResponse;
import com.fleebug.corerouter.entity.user.User;
import com.fleebug.corerouter.enums.user.UserStatus;
import com.fleebug.corerouter.exception.user.InvalidCredentialsException;
import com.fleebug.corerouter.exception.user.InvalidOtpException;
import com.fleebug.corerouter.exception.user.UserAlreadyExistsException;
import com.fleebug.corerouter.exception.user.UserNotFoundException;
import com.fleebug.corerouter.repository.user.UserRepository;
import com.fleebug.corerouter.service.otp.OtpService;
import com.fleebug.corerouter.service.token.TokenService;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final OtpService otpService;

    /**
     * Step 1: Request OTP for user registration
     * 
     * Takes only email address, validates it's not already registered,
     * generates and sends OTP, returns verificationId for next step.
     * 
     * Security Model:
     * - OTP Validity: 5 minutes
     * - Max Verify Attempts: 5
     * 
     * @param email Email address to request OTP for
     * @return RequestOtpResponse with verificationId
     * @throws IllegalArgumentException if email already exists or validation fails
     */
    public RequestOtpResponse requestOtp(String email) {
        log.debug("OTP request for registration");

        // Validate email is not already registered
        if (userRepository.existsByEmail(email)) {
            log.warn("OTP request failed - email already exists");
            throw new UserAlreadyExistsException(email);
        }

        log.debug("Email validation passed. Proceeding with OTP generation");

        String verificationId = otpService.requestOtp(email);
        log.info("OTP sent successfully. VerificationId: {}", verificationId);
        
        return RequestOtpResponse.builder()
                .verificationId(verificationId)
                .message("OTP sent to " + email)
                .ttlMinutes(5)
                .build();
    }

    /**
     * Step 2: Verify OTP
     * 
     * Takes verificationId and OTP, validates OTP matches the verificationId,
     * returns verification status for profile completion.
     * Does NOT create user yet - user creation happens in finalRegister().
     * 
     * @param verificationId Verification ID from Step 1 response
     * @param otp OTP from email
     * @return VerifyOtpResponse with verified status and verificationId
     * @throws IllegalArgumentException if OTP is invalid or expired
     */
    public VerifyOtpResponse verifyOtp(String verificationId, String otp) {
        log.debug("Verifying OTP with verificationId: {}", verificationId);

        String email = otpService.validateOtp(verificationId, otp);
        log.info("OTP verified successfully for verificationId: {}", verificationId);

        return VerifyOtpResponse.builder()
                .verificationId(verificationId)
                .message("OTP verified successfully. Complete your profile within 20 minutes.")
                .verified(true)
                .profileCompletionTtlMinutes(20)
                .build();
    }

    /**
     * Step 3: Final registration - creates user after OTP verification
     * 
     * Takes verified verificationId and profile data, creates user with provided information.
     * Validates that verificationId is verified, retrieves email, creates user, and returns tokens.
     * 
     * @param verificationId Verified verificationId from Step 2
     * @param finalRequest Contains profile data (fullName, password, profileImage, emailSubscribed)
     * @return AuthResponse with tokens and user details
     * @throws IllegalArgumentException if verificationId not verified or user creation fails
     */
    public AuthResponse finalRegister(String verificationId, FinalRegistrationRequest finalRequest) {
        log.debug("Final registration initiated for verificationId: {}", verificationId);

        // Step 1: Verify that the verificationId is verified
        if (!otpService.isVerified(verificationId)) {
            log.warn("Final registration failed - verificationId not verified: {}", verificationId);
            throw new InvalidOtpException("Verification not completed. Please verify OTP first.");
        }

        // Step 2: Get email from verificationId
        String email = otpService.getEmail(verificationId);
        log.debug("Retrieved email for verificationId: {}", verificationId);

        // Step 3: Hash password using BCrypt
        String hashedPassword = passwordEncoder.encode(finalRequest.getPassword());

        // Step 4: Create new user with profile data
        User user = User.builder()
                .fullName(finalRequest.getFullName())
                .email(email)
                .password(hashedPassword)
                .profileImage(finalRequest.getProfileImage())
                .emailSubscribed(finalRequest.isEmailSubscribed())
                .status(UserStatus.ACTIVE)
                .build();

        User savedUser = userRepository.save(user);
        log.info("User registered successfully via verification flow. User ID: {}", savedUser.getUserId());

        // Step 5: Cleanup verification data from Redis
        otpService.cleanupVerification(verificationId);
        log.info("Verification cleanup completed for verificationId: {}", verificationId);

        return tokenService.buildAuthResponse(savedUser);
    }

    /**
     * Login user with email and password
     * 
     * @param loginRequest contains email and password
     * @return AuthResponse with user details and success status
     * @throws UserNotFoundException if user not found
     * @throws InvalidCredentialsException if password is incorrect
     */
    public AuthResponse login(LoginRequest loginRequest) {
        log.debug("Login attempt for user");

        // Find user by email
        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> {
                    log.warn("Login failed - user not found");
                    return new UserNotFoundException("email", loginRequest.getEmail());
                });

        // Check if user is active
        if (user.getStatus() != UserStatus.ACTIVE) {
            log.warn("Login failed - user account is not active. UserId: {}, Status: {}", 
                    user.getUserId(), user.getStatus());
            throw new InvalidCredentialsException("User account is not active");
        }

        // Verify password using BCrypt
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            log.warn("Login failed - invalid password for user: {}", user.getUserId());
            throw new InvalidCredentialsException();
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
                    return new UserNotFoundException(email);
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
                    return new UserNotFoundException(userId);
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
            throw new InvalidCredentialsException("Invalid old password");
        }

        // Hash and update new password
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("Password changed successfully for user ID: {}", userId);

        return tokenService.buildAuthResponse(user);
    }

  }