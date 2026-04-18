package com.fleebug.corerouter.service.user;

import com.fleebug.corerouter.dto.user.response.AuthResponse;
import com.fleebug.corerouter.enums.activity.ActivityAction;
import com.fleebug.corerouter.entity.user.User;
import com.fleebug.corerouter.enums.user.UserRole;
import com.fleebug.corerouter.enums.user.UserStatus;
import com.fleebug.corerouter.exception.user.InvalidCredentialsException;
import com.fleebug.corerouter.exception.user.UserNotFoundException;
import com.fleebug.corerouter.repository.token.UserTokenRepository;
import com.fleebug.corerouter.repository.user.UserRepository;
import com.fleebug.corerouter.service.activity.ActivityLogService;
import com.fleebug.corerouter.service.otp.OtpService;
import com.fleebug.corerouter.service.token.TokenService;
import com.fleebug.corerouter.util.HttpClientUtil;
import com.microsoft.applicationinsights.TelemetryClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for user changing password.
 */
class UserServiceChangePasswordTest {

    private UserService userService;

    private TelemetryClient telemetryClient;
    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private TokenService tokenService;
    private OtpService otpService;
    private UserTokenRepository userTokenRepository;
    private ActivityLogService activityLogService;
    private HttpClientUtil httpClientUtil;

    private User activeUser;

    @BeforeEach
    void setUp() {
        telemetryClient = mock(TelemetryClient.class);
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        tokenService = mock(TokenService.class);
        otpService = mock(OtpService.class);
        userTokenRepository = mock(UserTokenRepository.class);
        activityLogService = mock(ActivityLogService.class);
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

        activeUser = User.builder()
                .userId(1)
                .email("user@example.com")
                .fullName("Test User")
                .password("hashed_old_password")
                .status(UserStatus.ACTIVE)
                .role(UserRole.USER)
                .build();
    }

    @Test
    // Tests that changing password from an active logged-in user fails if passwords match
    void changePassword_whenNewPasswordMatchesOld_throwsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                userService.changePassword(1, "samePassword", "samePassword", "127.0.0.1")
        );

        assertEquals("New password must be different from the current password", ex.getMessage());
        verifyNoInteractions(userRepository);
    }

    @Test
    // Tests that passing an invalid old password throws InvalidCredentialsException
    void changePassword_whenOldPasswordIncorrect_throwsInvalidCredentialsException() {
        when(userRepository.findById(1)).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("wrongOldPassword", 
            "hashed_old_password")).thenReturn(false);

        InvalidCredentialsException ex = assertThrows(InvalidCredentialsException.class, () ->
                userService.changePassword(1, "wrongOldPassword", 
                    "newPassword", "127.0.0.1")
        );

        assertEquals("Invalid old password", ex.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    // Tests that a valid password change encrypts the new password, logs activity, and sends notification
    void changePassword_whenValid_updatesPasswordAndSendsNotification() {
        when(userRepository.findById(1)).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("correctOldPassword", 
        "hashed_old_password")).thenReturn(true);
        when(passwordEncoder.encode("newPassword123")).thenReturn("hashed_new_password");

        AuthResponse mockAuthResponse = new AuthResponse();
        mockAuthResponse.setAccessToken("new_access_token");
        when(tokenService.buildAuthResponse(activeUser)).thenReturn(mockAuthResponse);

        AuthResponse res = userService.changePassword(1, "correctOldPassword", 
        "newPassword123", "127.0.0.1");

        assertNotNull(res);
        assertEquals("new_access_token", res.getAccessToken());

        // Password should have been updated with the encoded hash
        assertEquals("hashed_new_password", activeUser.getPassword());
        verify(userRepository).save(activeUser);

        // Verification that an activity log got registered
        verify(activityLogService).log(activeUser, ActivityAction.CHANGE_PASSWORD, 
            "Your password was changed successfully.", "127.0.0.1");

        // Verification that notification publisher was triggered
        verify(otpService).publishPasswordChangedNotification("user@example.com", "Test User", 1);
    }

    @Test
    // Tests that a non-existent user throws UserNotFoundException
    void changePassword_whenUserNotFound_throwsUserNotFoundException() {
        when(userRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () ->
                userService.changePassword(999, "oldPassword", "newPassword", "127.0.0.1")
        );
    }
}
