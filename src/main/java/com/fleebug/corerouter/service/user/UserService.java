package com.fleebug.corerouter.service.user;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.SeverityLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.fleebug.corerouter.dto.otp.FinalRegistrationRequest;
import com.fleebug.corerouter.dto.otp.RequestOtpResponse;
import com.fleebug.corerouter.dto.otp.VerifyOtpResponse;
import com.fleebug.corerouter.dto.user.request.ChangePasswordRequest;
import com.fleebug.corerouter.dto.user.request.LoginRequest;
import com.fleebug.corerouter.dto.user.request.UpdateProfileRequest;
import com.fleebug.corerouter.dto.user.response.AdminUserInsightsResponse;
import com.fleebug.corerouter.dto.user.response.AuthResponse;
import com.fleebug.corerouter.dto.user.response.UserProfileResponse;
import com.fleebug.corerouter.dto.user.response.UserAnalyticsResponse;
import com.fleebug.corerouter.dto.user.response.DailyUserAnalyticsResponse;
import com.fleebug.corerouter.dto.user.response.PaginatedUserListResponse;
import com.fleebug.corerouter.entity.token.UserToken;
import com.fleebug.corerouter.entity.user.User;
import com.fleebug.corerouter.enums.activity.ActivityAction;
import com.fleebug.corerouter.enums.otp.OtpPurpose;
import com.fleebug.corerouter.enums.user.UserRole;
import com.fleebug.corerouter.enums.user.UserStatus;
import com.fleebug.corerouter.exception.user.InvalidCredentialsException;
import com.fleebug.corerouter.exception.user.InvalidOtpException;
import com.fleebug.corerouter.exception.user.UserAlreadyExistsException;
import com.fleebug.corerouter.exception.user.UserNotFoundException;
import com.fleebug.corerouter.repository.token.UserTokenRepository;
import com.fleebug.corerouter.repository.user.UserRepository;
import com.fleebug.corerouter.service.activity.ActivityLogService;
import com.fleebug.corerouter.service.otp.OtpService;
import com.fleebug.corerouter.service.token.TokenService;
import com.fleebug.corerouter.util.HttpClientUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final TelemetryClient telemetryClient;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final OtpService otpService;
    private final UserTokenRepository userTokenRepository;
    private final ActivityLogService activityLogService;
    private final HttpClientUtil httpClientUtil;

    @Value("${auth.oauth.google.token-url:https://oauth2.googleapis.com/token}")
    private String googleTokenUrl;

    @Value("${auth.oauth.google.redirect-uri:http://localhost:3000/auth/callback}")
    private String googleRedirectUri;

    @Value("${spring.security.oauth2.client.registration.google.client-id:}")
    private String googleClientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret:}")
    private String googleClientSecret;

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
    public RequestOtpResponse requestOtp(String email, OtpPurpose purpose) {
        // telemetryClient.trackTrace("OTP request for registration", SeverityLevel.Verbose, null);

        // Validate email is not already registered
        if (userRepository.existsByEmail(email)) {
            telemetryClient.trackTrace("OTP request failed - email already exists", SeverityLevel.Information, null);
            throw new UserAlreadyExistsException(email);
        }

        // telemetryClient.trackTrace("Email validation passed. Proceeding with OTP generation", SeverityLevel.Verbose, null);

        String verificationId = otpService.requestOtp(email, purpose, null, null);
        telemetryClient.trackTrace("OTP sent successfully. VerificationId: " + verificationId, SeverityLevel.Information, Map.of("verificationId", verificationId));
        
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
        // telemetryClient.trackTrace("Verifying OTP with verificationId: " + verificationId, SeverityLevel.Verbose, Map.of("verificationId", verificationId));

        otpService.validateOtp(verificationId, otp);
        telemetryClient.trackTrace("OTP verified successfully for verificationId: " + verificationId, SeverityLevel.Information, Map.of("verificationId", verificationId));

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
        // telemetryClient.trackTrace("Final registration initiated for verificationId: " + verificationId, SeverityLevel.Verbose, Map.of("verificationId", verificationId));

        // Step 1: Verify that the verificationId is verified
        if (!otpService.isVerified(verificationId)) {
            telemetryClient.trackTrace("Final registration failed - verificationId not verified: " + verificationId, SeverityLevel.Information, Map.of("verificationId", verificationId));
            throw new InvalidOtpException("Verification not completed. Please verify OTP first.");
        }

        // Step 2: Get email from verificationId
        String email = otpService.getEmail(verificationId);
        // telemetryClient.trackTrace("Retrieved email for verificationId: " + verificationId, SeverityLevel.Verbose, Map.of("verificationId", verificationId));

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
        telemetryClient.trackTrace("User registered successfully via verification flow. User ID: " + savedUser.getUserId(), SeverityLevel.Information, Map.of("userId", String.valueOf(savedUser.getUserId())));

        // Step 5: Cleanup verification data from Redis
        otpService.cleanupVerification(verificationId);
        telemetryClient.trackTrace("Verification cleanup completed for verificationId: " + verificationId, SeverityLevel.Information, Map.of("verificationId", verificationId));

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
        // telemetryClient.trackTrace("Login attempt for user", SeverityLevel.Verbose, null);

        // Find user by email
        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> {
                    telemetryClient.trackTrace("Login failed - user not found", SeverityLevel.Information, null);
                    return new UserNotFoundException("email", loginRequest.getEmail());
                });

        // Check if user is active
        if (user.getStatus() != UserStatus.ACTIVE) {
            telemetryClient.trackTrace("Login failed - user account is not active. UserId: " + user.getUserId() + ", Status: " + user.getStatus(), SeverityLevel.Information, Map.of("userId", String.valueOf(user.getUserId()), "status", user.getStatus().toString()));
            throw new InvalidCredentialsException("User account is not active");
        }

        if (user.getPassword() == null || user.getPassword().isBlank()) {
            telemetryClient.trackTrace("Login failed - account does not support password login", SeverityLevel.Information,
                    Map.of("userId", String.valueOf(user.getUserId()), "email", user.getEmail()));
            throw new InvalidCredentialsException("Use social login for this account");
        }

        // Verify password using BCrypt
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            telemetryClient.trackTrace("Login failed - invalid password for user: " + user.getUserId(), SeverityLevel.Information, Map.of("userId", String.valueOf(user.getUserId())));
            throw new InvalidCredentialsException();
        }

        telemetryClient.trackTrace("User logged in successfully. User ID: " + user.getUserId(), SeverityLevel.Information, Map.of("userId", String.valueOf(user.getUserId())));

        return tokenService.buildAuthResponse(user);
    }

    public AuthResponse loginWithGoogle(String accessToken) {
        Map<String, String> headers = Map.of("Authorization", "Bearer " + accessToken);
        Map<String, Object> profile = httpClientUtil.getJsonMap(
                "https://www.googleapis.com/oauth2/v3/userinfo",
                headers,
                5000,
                10000
        );

        String email = asString(profile.get("email"));
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Google account email not available");
        }

        String fullName = firstNonBlank(asString(profile.get("name")), email.split("@")[0]);
        String profileImage = asString(profile.get("picture"));

        return upsertSocialUserAndLogin(email, fullName, profileImage, "GOOGLE");
    }

    public AuthResponse loginWithGoogleCode(String code) {
        if (googleClientId == null || googleClientId.isBlank() || googleClientSecret == null || googleClientSecret.isBlank()) {
            throw new IllegalStateException("Google OAuth credentials are not configured");
        }

        Map<String, String> formData = Map.of(
                "client_id", googleClientId,
                "client_secret", googleClientSecret,
                "code", code,
                "redirect_uri", googleRedirectUri,
                "grant_type", "authorization_code"
        );

        Map<String, Object> tokenResponse = httpClientUtil.postFormForMap(
                googleTokenUrl,
                Map.of("Accept", "application/json"),
                formData,
                5000,
                10000
        );

        String accessToken = asString(tokenResponse.get("access_token"));
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException("Google token exchange failed: access token missing");
        }

        return loginWithGoogle(accessToken);
    }

    public AuthResponse loginWithGithub(String accessToken) {
        Map<String, String> headers = Map.of(
                "Authorization", "Bearer " + accessToken,
                "Accept", "application/vnd.github+json",
                "X-GitHub-Api-Version", "2022-11-28"
        );

        Map<String, Object> profile = httpClientUtil.getJsonMap(
                "https://api.github.com/user",
                headers,
                5000,
                10000
        );

        String email = asString(profile.get("email"));
        if (email == null || email.isBlank()) {
            email = resolveGithubPrimaryEmail(headers);
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("GitHub account email not available");
        }

        String fullName = firstNonBlank(asString(profile.get("name")), asString(profile.get("login")), email.split("@")[0]);
        String profileImage = asString(profile.get("avatar_url"));

        return upsertSocialUserAndLogin(email, fullName, profileImage, "GITHUB");
    }

    public RequestOtpResponse requestPasswordResetOtp(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> {
                telemetryClient.trackTrace("Forgot password OTP request failed - user not found", SeverityLevel.Information,
                    Map.of("email", email));
                return new UserNotFoundException("email", email);
            });

        String verificationId = otpService.requestOtp(email, OtpPurpose.PASSWORD_RESET, user.getFullName(), user.getUserId());
        telemetryClient.trackTrace("Forgot password OTP sent", SeverityLevel.Information,
                Map.of("verificationId", verificationId, "email", email));

        return RequestOtpResponse.builder()
                .verificationId(verificationId)
                .message("OTP sent to " + email)
                .ttlMinutes(5)
                .build();
    }

    public VerifyOtpResponse verifyPasswordResetOtp(String verificationId, String otp) {
        otpService.validateOtp(verificationId, otp);
        telemetryClient.trackTrace("Forgot password OTP verified", SeverityLevel.Information,
                Map.of("verificationId", verificationId));

        return VerifyOtpResponse.builder()
                .verificationId(verificationId)
                .message("OTP verified successfully. Complete password reset within 20 minutes.")
                .verified(true)
                .profileCompletionTtlMinutes(20)
                .build();
    }

    public void resetPasswordWithVerification(String verificationId, String newPassword, String confirmPassword) {
        if (!Objects.equals(newPassword, confirmPassword)) {
            throw new IllegalArgumentException("New password and confirm password do not match");
        }

        if (!otpService.isVerified(verificationId)) {
            throw new InvalidOtpException("Verification not completed. Please verify OTP first.");
        }

        String email = otpService.getEmail(verificationId);
        if (email == null || email.isBlank()) {
            throw new InvalidOtpException("Verification session expired. Please request OTP again.");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("email", email));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        otpService.publishPasswordChangedNotification(user.getEmail(), user.getFullName(), user.getUserId());

        otpService.cleanupVerification(verificationId);
        telemetryClient.trackTrace("Password reset completed", SeverityLevel.Information,
                Map.of("userId", String.valueOf(user.getUserId()), "email", email));
    }

    private AuthResponse upsertSocialUserAndLogin(String email, String fullName, String profileImage, String provider) {
        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User created = User.builder()
                    .email(email)
                    .fullName(fullName)
                    .profileImage(profileImage)
                    .status(UserStatus.ACTIVE)
                    .role(UserRole.USER)
                    .password(null)
                    .emailSubscribed(true)
                    .build();
            return userRepository.save(created);
        });

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new InvalidCredentialsException("User account is not active");
        }

        if ((user.getFullName() == null || user.getFullName().isBlank()) && fullName != null && !fullName.isBlank()) {
            user.setFullName(fullName);
        }
        if ((user.getProfileImage() == null || user.getProfileImage().isBlank()) && profileImage != null && !profileImage.isBlank()) {
            user.setProfileImage(profileImage);
        }
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        telemetryClient.trackTrace("Social login successful", SeverityLevel.Information,
                Map.of("userId", String.valueOf(user.getUserId()), "provider", provider));

        return tokenService.buildAuthResponse(user);
    }

    private String resolveGithubPrimaryEmail(Map<String, String> headers) {
        Object raw = httpClientUtil.getJson("https://api.github.com/user/emails", headers, Object.class, 5000, 10000);
        if (!(raw instanceof List<?> emails)) {
            return null;
        }

        for (Object entry : emails) {
            if (entry instanceof Map<?, ?> map) {
                Object primaryValue = map.get("primary");
                Object verifiedValue = map.get("verified");
                boolean primary = Boolean.TRUE.equals(primaryValue);
                boolean verified = Boolean.TRUE.equals(verifiedValue);
                if (primary && verified) {
                    return asString(map.get("email"));
                }
            }
        }

        for (Object entry : emails) {
            if (entry instanceof Map<?, ?> map) {
                boolean verified = Boolean.TRUE.equals(map.get("verified"));
                if (verified) {
                    return asString(map.get("email"));
                }
            }
        }

        return null;
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    /**
     * Get user by email
     * 
     * @param email user email
     * @return User object if found
     * @throws IllegalArgumentException if user not found
     */
    public User getUserByEmail(String email) {
        telemetryClient.trackTrace("Fetching user with email: " + email, SeverityLevel.Information, Map.of("email", email));
        return userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    telemetryClient.trackTrace("User not found with email: " + email, SeverityLevel.Information, Map.of("email", email));
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
        telemetryClient.trackTrace("Fetching user with ID: " + userId, SeverityLevel.Information, Map.of("userId", String.valueOf(userId)));
        return userRepository.findById(userId)
                .orElseThrow(() -> {
                    telemetryClient.trackTrace("User not found with ID: " + userId, SeverityLevel.Information, Map.of("userId", String.valueOf(userId)));
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
        return changePassword(userId, oldPassword, newPassword, "UNKNOWN");
    }

    public AuthResponse changePassword(Integer userId, String oldPassword, String newPassword, String ipAddress) {
        telemetryClient.trackTrace("Attempting to change password", SeverityLevel.Information, Map.of("userId", String.valueOf(userId)));

        if (oldPassword != null && oldPassword.equals(newPassword)) {
            telemetryClient.trackTrace("Change password failed - new password matches current password", SeverityLevel.Information, Map.of("userId", String.valueOf(userId)));
            throw new IllegalArgumentException("New password must be different from the current password");
        }

        User user = getUserById(userId);

        // Verify old password
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            telemetryClient.trackTrace("Change password failed - invalid old password", SeverityLevel.Information, Map.of("userId", String.valueOf(userId)));
            throw new InvalidCredentialsException("Invalid old password");
        }

        // Hash and update new password
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        otpService.publishPasswordChangedNotification(user.getEmail(), user.getFullName(), user.getUserId());
        telemetryClient.trackTrace("Password changed successfully", SeverityLevel.Information, Map.of("userId", String.valueOf(userId)));

        activityLogService.log(user, ActivityAction.CHANGE_PASSWORD, "Your password was changed successfully.", ipAddress);

        return tokenService.buildAuthResponse(user);
    }

    /**
     * Get profile details for authenticated user.
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getMyProfile(Integer userId) {
        User user = getUserById(userId);
        return mapToProfileResponse(user);
    }

    /**
     * Update profile for authenticated user.
     */
    public UserProfileResponse updateProfile(Integer userId, UpdateProfileRequest request, String ipAddress) {
        telemetryClient.trackTrace("Updating user profile", SeverityLevel.Information, Map.of("userId", String.valueOf(userId)));

        User user = getUserById(userId);

        if (user.getStatus() == UserStatus.DELETED) {
            throw new IllegalArgumentException("Deleted account cannot be updated");
        }

        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName().trim());
        }

        if (request.getProfileImage() != null) {
            user.setProfileImage(request.getProfileImage().trim().isEmpty() ? null : request.getProfileImage().trim());
        }

        if (request.getEmailSubscribed() != null) {
            user.setEmailSubscribed(request.getEmailSubscribed());
        }

        User updated = userRepository.save(user);
        telemetryClient.trackTrace("User profile updated successfully", SeverityLevel.Information, Map.of("userId", String.valueOf(userId)));

        activityLogService.log(updated, ActivityAction.UPDATE_PROFILE, "Your profile and email preferences were updated successfully.", ipAddress);

        return mapToProfileResponse(updated);
    }

    /**
     * Change password for authenticated user using request payload.
     */
    public void changePassword(Integer userId, ChangePasswordRequest request, String ipAddress) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("New password and confirm password do not match");
        }

        changePassword(userId, request.getCurrentPassword(), request.getNewPassword(), ipAddress);
    }

    /**
     * Soft delete account while preserving historical records.
     */
    public void softDeleteAccount(Integer userId, String currentPassword, String ipAddress) {
        telemetryClient.trackTrace("Soft delete account initiated", SeverityLevel.Information, Map.of("userId", String.valueOf(userId)));

        User user = getUserById(userId);

        if (user.getStatus() == UserStatus.DELETED) {
            throw new IllegalArgumentException("Account is already deleted");
        }

        if (user.getPassword() != null && !passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new InvalidCredentialsException("Invalid password");
        }

        user.setStatus(UserStatus.DELETED);
        user.setEmailSubscribed(false);
        userRepository.save(user);
        otpService.publishUserDeletedNotification(user.getEmail(), user.getFullName(), user.getUserId(), "self-service delete");

        // Revoke all active tokens to force logout from all sessions.
        for (UserToken token : userTokenRepository.findByUserAndRevokedFalse(user)) {
            token.setRevoked(true);
        }

        activityLogService.log(user, ActivityAction.SOFT_DELETE_ACCOUNT, "Your account was deleted.", ipAddress);

        telemetryClient.trackTrace("User account soft deleted", SeverityLevel.Information, Map.of("userId", String.valueOf(userId)));
    }

    @Transactional(readOnly = true)
    public AdminUserInsightsResponse getAdminInsights() {
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByStatus(UserStatus.ACTIVE);
        long inactiveUsers = userRepository.countByStatus(UserStatus.INACTIVE);
        long suspendedUsers = userRepository.countByStatus(UserStatus.SUSPENDED);
        long adminUsers = userRepository.countByRole(UserRole.ADMIN);

        LocalDateTime currentMonthStart = LocalDateTime.now()
                .withDayOfMonth(1)
                .withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
        long pastMonthBaseline = userRepository.countByCreatedAtBefore(currentMonthStart);

        BigDecimal usersChangeFromPastMonthPercent = BigDecimal.ZERO;
        if (pastMonthBaseline > 0) {
            usersChangeFromPastMonthPercent = BigDecimal.valueOf(totalUsers - pastMonthBaseline)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(pastMonthBaseline), 1, RoundingMode.HALF_UP);
        }

        BigDecimal activeSharePercent = BigDecimal.ZERO;
        if (totalUsers > 0) {
            activeSharePercent = BigDecimal.valueOf(activeUsers)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalUsers), 1, RoundingMode.HALF_UP);
        }

        return AdminUserInsightsResponse.builder()
                .totalUsers(totalUsers)
                .usersChangeFromPastMonthPercent(usersChangeFromPastMonthPercent)
                .activeUsers(activeUsers)
                .inactiveUsers(inactiveUsers)
                .activeSharePercent(activeSharePercent)
                .suspendedUsers(suspendedUsers)
                .adminUsers(adminUsers)
                .build();
    }

    /**
     * Get user analytics for a date range
     * Returns daily counts of created, deleted, and revoked (suspended/banned) users
     *
     * @param fromDate Start date (inclusive)
     * @param toDate End date (inclusive)
     * @return UserAnalyticsResponse with daily breakdown
     */
    @Transactional(readOnly = true)
    public UserAnalyticsResponse getUserAnalyticsByDateRange(LocalDateTime fromDate, LocalDateTime toDate) {
        telemetryClient.trackTrace("Fetching user analytics from " + fromDate + " to " + toDate, SeverityLevel.Information, 
            Map.of("fromDate", fromDate.toString(), "toDate", toDate.toString()));

        long totalCreated = userRepository.countByStatusAndCreatedAtBetween(null, fromDate, toDate);
        // Since User entity doesn't have explicit deletedAt, we count users with DELETED status created in range
        long totalDeleted = userRepository.countByStatusAndCreatedAtBetween(UserStatus.DELETED, fromDate, toDate);
        // Revoked = SUSPENDED or BANNED users
        long totalSuspended = userRepository.countByStatusAndCreatedAtBetween(UserStatus.SUSPENDED, fromDate, toDate);
        long totalBanned = userRepository.countByStatusAndCreatedAtBetween(UserStatus.BANNED, fromDate, toDate);
        long totalRevoked = totalSuspended + totalBanned;

        List<DailyUserAnalyticsResponse> dailyAnalytics = new java.util.ArrayList<>();
        java.time.LocalDate currentDate = fromDate.toLocalDate();
        java.time.LocalDate endDate = toDate.toLocalDate();

        // Build daily breakdown
        while (!currentDate.isAfter(endDate)) {
            LocalDateTime dayStart = currentDate.atStartOfDay();
            LocalDateTime dayEnd = currentDate.atTime(23, 59, 59);

            long dayCreated = countUsersByCreatedAtBetween(dayStart, dayEnd);
            long dayDeleted = userRepository.countByStatusAndCreatedAtBetween(UserStatus.DELETED, dayStart, dayEnd);
            long daySuspended = userRepository.countByStatusAndCreatedAtBetween(UserStatus.SUSPENDED, dayStart, dayEnd);
            long dayBanned = userRepository.countByStatusAndCreatedAtBetween(UserStatus.BANNED, dayStart, dayEnd);
            long dayRevoked = daySuspended + dayBanned;

            if (dayCreated > 0 || dayDeleted > 0 || dayRevoked > 0) {
                dailyAnalytics.add(DailyUserAnalyticsResponse.builder()
                        .date(currentDate)
                        .created(dayCreated)
                        .deleted(dayDeleted)
                        .revoked(dayRevoked)
                        .build());
            }

            currentDate = currentDate.plusDays(1);
        }

        return UserAnalyticsResponse.builder()
                .dailyAnalytics(dailyAnalytics)
                .totalCreated(totalCreated)
                .totalDeleted(totalDeleted)
                .totalRevoked(totalRevoked)
                .build();
    }

    /**
     * Get paginated list of users with optional role and status filters
     *
     * @param page Page number (0-indexed)
     * @param size Page size
     * @param role Filter by role (USER, ADMIN, or null for all)
     * @param status Filter by status (ACTIVE, INACTIVE, etc., or null for all)
     * @return PaginatedUserListResponse with user list and pagination info
     */
    @Transactional(readOnly = true)
    public PaginatedUserListResponse getUsersWithFilters(int page, int size, UserRole role, UserStatus status) {
        telemetryClient.trackTrace("Fetching paginated users - page: " + page + ", size: " + size + 
            (role != null ? ", role: " + role : "") + (status != null ? ", status: " + status : ""),
            SeverityLevel.Information, Map.of("page", String.valueOf(page), "size", String.valueOf(size)));

        Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        Page<User> userPage;

        if (role != null && status != null) {
            userPage = userRepository.findByRoleAndStatus(role, status, pageable);
        } else if (role != null) {
            userPage = userRepository.findByRole(role, pageable);
        } else if (status != null) {
            userPage = userRepository.findByStatus(status, pageable);
        } else {
            userPage = userRepository.findAll(pageable);
        }

        Page<UserProfileResponse> responsePage = userPage.map(this::mapToProfileResponse);
        return PaginatedUserListResponse.fromPage(responsePage);
    }

    /**
     * Admin-only status update for any user.
     */
    public UserProfileResponse updateUserStatusByAdmin(Integer userId, UserStatus newStatus) {
        telemetryClient.trackTrace("Admin updating user status", SeverityLevel.Information,
                Map.of("userId", String.valueOf(userId), "newStatus", newStatus.name()));

        User user = getUserById(userId);

        if (user.getStatus() == newStatus) {
            throw new IllegalArgumentException("User already has status " + newStatus);
        }

        user.setStatus(newStatus);

        // Keep subscription disabled for deleted users.
        if (newStatus == UserStatus.DELETED) {
            user.setEmailSubscribed(false);
        }

        User saved = userRepository.save(user);
        if (newStatus == UserStatus.DELETED) {
            otpService.publishUserDeletedNotification(saved.getEmail(), saved.getFullName(), saved.getUserId(), "admin");
        }
        return mapToProfileResponse(saved);
    }

    /**
     * Count all users created within a date range (any status)
     * Helper method for analytics
     */
    private long countUsersByCreatedAtBetween(LocalDateTime from, LocalDateTime to) {
        // Since we only have createdAt, we count all users in that range
        return userRepository.countByCreatedAtBetween(from, to);
    }


    private UserProfileResponse mapToProfileResponse(User user) {
        return UserProfileResponse.builder()
                .userId(user.getUserId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .profileImage(user.getProfileImage())
            .emailSubscribed(user.isEmailSubscribed())
                .status(user.getStatus())
                .build();
    }

  }