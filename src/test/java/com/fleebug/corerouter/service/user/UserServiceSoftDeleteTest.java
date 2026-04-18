package com.fleebug.corerouter.service.user;

import com.fleebug.corerouter.entity.token.UserToken;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for soft deleting user accounts.
 */
class UserServiceSoftDeleteTest {

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
                .password("hashed_password")
                .status(UserStatus.ACTIVE)
                .role(UserRole.USER)
                .emailSubscribed(true)
                .build();
    }

    @Test
    // Tests that attempting to delete an already deleted account throws an exception
    void softDeleteAccount_whenAlreadyDeleted_throwsIllegalArgumentException() {
        activeUser.setStatus(UserStatus.DELETED);
        when(userRepository.findById(1)).thenReturn(Optional.of(activeUser));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
            userService.softDeleteAccount(1, "password", "127.0.0.1")
        );

        assertEquals("Account is already deleted", ex.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    // Tests that providing the incorrect current password blocks the soft delete
    void softDeleteAccount_whenPasswordIncorrect_throwsInvalidCredentialsException() {
        when(userRepository.findById(1)).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("wrongPassword", "hashed_password")).thenReturn(false);

        InvalidCredentialsException ex = assertThrows(InvalidCredentialsException.class, () ->
                userService.softDeleteAccount(1, "wrongPassword", "127.0.0.1")
        );

        assertEquals("Invalid password", ex.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    // Tests that a valid delete request correctly updates the status, flags tokens as revoked, and logs the deletion
    void softDeleteAccount_whenValid_updatesStatusRevokesTokensAndSendsNotification() {
        when(userRepository.findById(1)).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("correctPassword", "hashed_password"))
                .thenReturn(true);

        UserToken token1 = new UserToken();
        token1.setRevoked(false);
        UserToken token2 = new UserToken();
        token2.setRevoked(false);

        when(userTokenRepository.findByUserAndRevokedFalse(activeUser))
                .thenReturn(List.of(token1, token2));

        userService.softDeleteAccount(1, "correctPassword", "127.0.0.1");

        // Verify status and subscription correctly changed in memory
        assertEquals(UserStatus.DELETED, activeUser.getStatus());
        assertFalse(activeUser.isEmailSubscribed());
        
        // Verify changes are sent to DB
        verify(userRepository).save(activeUser);

        // Verify all live tokens got revoked
        assertTrue(token1.isRevoked());
        assertTrue(token2.isRevoked());

        // Verify email notification is triggered
        verify(otpService).publishUserDeletedNotification("user@example.com", "Test User",
         1, "self-service delete");

        // Verify Activity Log is executed with proper IP address
        verify(activityLogService).log(activeUser, com.fleebug.corerouter.enums.activity.ActivityAction.SOFT_DELETE_ACCOUNT,
             "Your account was deleted.", "127.0.0.1");
    }

    @Test
    // Tests that soft deleting a social login (no password) works without executing match checks
    void softDeleteAccount_whenNoPasswordSet_ignoresPasswordCheck() {
        activeUser.setPassword(null); // e.g. Social Login
        when(userRepository.findById(1)).thenReturn(Optional.of(activeUser));
        when(userTokenRepository.findByUserAndRevokedFalse(activeUser)).thenReturn(new ArrayList<>());

        userService.softDeleteAccount(1, "whatever", "127.0.0.1");

        // Since old password was null, it shouldn't even trigger the matches() comparison
        verify(passwordEncoder, never()).matches(anyString(), anyString());

        assertEquals(UserStatus.DELETED, activeUser.getStatus());
        verify(userRepository).save(activeUser);
    }

    @Test
    // Tests that soft deleting a non-existent user throws an exception
    void softDeleteAccount_whenUserNotFound_throwsUserNotFoundException() {
        when(userRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () ->
            userService.softDeleteAccount(999, "pwd", "127.0.0.1")
        );
    }
}
