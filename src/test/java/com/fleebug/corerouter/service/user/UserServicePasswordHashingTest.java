package com.fleebug.corerouter.service.user;

import com.fleebug.corerouter.dto.otp.FinalRegistrationRequest;
import com.fleebug.corerouter.entity.user.User;
import com.fleebug.corerouter.enums.user.UserStatus;
import com.fleebug.corerouter.repository.user.UserRepository;
import com.fleebug.corerouter.service.otp.OtpService;
import com.fleebug.corerouter.service.token.TokenService;
import com.fleebug.corerouter.service.activity.ActivityLogService;
import com.fleebug.corerouter.repository.token.UserTokenRepository;
import com.fleebug.corerouter.util.HttpClientUtil;
import com.microsoft.applicationinsights.TelemetryClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests focused on password hashing logic (no HTTP or database).
 */
class UserServicePasswordHashingTest {

    private UserService userService;

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private OtpService otpService;
    private TokenService tokenService;
    private TelemetryClient telemetryClient;
    private ActivityLogService activityLogService;
    private UserTokenRepository userTokenRepository;
    private HttpClientUtil httpClientUtil;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        otpService = mock(OtpService.class);
        tokenService = mock(TokenService.class);
        telemetryClient = mock(TelemetryClient.class);
        activityLogService = mock(ActivityLogService.class);
        userTokenRepository = mock(UserTokenRepository.class);
        httpClientUtil = mock(HttpClientUtil.class);

        userService = new UserService(
            telemetryClient,
            userRepository,
            passwordEncoder,
            tokenService,
            otpService,
            userTokenRepository,
            activityLogService,
            httpClientUtil
        );
    }

    // Final registration should hash the plain password before saving user
    @Test
    void finalRegister_hashesPasswordBeforeSavingUser() {
        String verificationId = "ver-123";
        String plainPassword = "MySecret123!";
        String encodedPassword = "{bcrypt}$2a$10$encoded";

        FinalRegistrationRequest request = new FinalRegistrationRequest();
        request.setVerificationId(verificationId);
        request.setFullName("Test User");
        request.setPassword(plainPassword);
        request.setConfirmPassword(plainPassword);
        request.setProfileImage(null);
        request.setEmailSubscribed(true);

        // verification is already done
        when(otpService.isVerified(verificationId)).thenReturn(true);
        when(otpService.getEmail(verificationId)).thenReturn("user@example.com");

        // encode should be called with raw password and return encoded value
        when(passwordEncoder.encode(plainPassword)).thenReturn(encodedPassword);

        // capture the user that is persisted
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setUserId(1);
            return u;
        });

        userService.finalRegister(verificationId, request);

        // ensure encode was called with plain password
        verify(passwordEncoder).encode(plainPassword);

        // ensure the saved user does not store the plain password
        verify(userRepository).save(argThat(user ->
                encodedPassword.equals(user.getPassword()) &&
                        !plainPassword.equals(user.getPassword()) &&
                        user.getStatus() == UserStatus.ACTIVE
        ));
    }

    // Resetting password with verification should store only the encoded password
    @Test
    void resetPasswordWithVerification_hashesNewPasswordBeforeSaving() {
        String verificationId = "ver-456";
        String newPassword = "NewPass#123";
        String encodedNewPassword = "{bcrypt}$2a$10$new";

        User existingUser = new User();
        existingUser.setUserId(2);
        existingUser.setEmail("reset@example.com");

        when(otpService.isVerified(verificationId)).thenReturn(true);
        when(otpService.getEmail(verificationId)).thenReturn("reset@example.com");
        when(userRepository.findByEmail("reset@example.com")).thenReturn(Optional.of(existingUser));

        when(passwordEncoder.encode(newPassword)).thenReturn(encodedNewPassword);

        userService.resetPasswordWithVerification(verificationId, newPassword, newPassword);

        verify(passwordEncoder).encode(newPassword);
        verify(userRepository).save(argThat(user ->
                encodedNewPassword.equals(user.getPassword()) &&
                        !newPassword.equals(user.getPassword())
        ));
    }
}
