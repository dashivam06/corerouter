package com.fleebug.corerouter.service.user;

import com.fleebug.corerouter.dto.user.request.LoginRequest;
import com.fleebug.corerouter.dto.user.response.AuthResponse;
import com.fleebug.corerouter.dto.user.response.AuthUserInfoResponse;
import com.fleebug.corerouter.entity.user.User;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the core email+password login flow.
 */
class UserServiceLoginTest {

    private UserService userService;

    private TelemetryClient telemetryClient;
    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private TokenService tokenService;
    private OtpService otpService;
    private UserTokenRepository userTokenRepository;
    private ActivityLogService activityLogService;
    private HttpClientUtil httpClientUtil;

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
    }

    // Happy path: active user with password and correct credentials
    @Test
    void login_whenCredentialsValid_returnsAuthResponse() {
        LoginRequest request = LoginRequest.builder()
                .email("john.doe@example.com")
                .password("P@ssword123")
                .build();

        User user = new User();
        user.setUserId(10);
        user.setEmail(request.getEmail());
        user.setPassword("hashed-password");
        user.setStatus(UserStatus.ACTIVE);

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.getPassword(), user.getPassword())).thenReturn(true);

        AuthUserInfoResponse profile = AuthUserInfoResponse.builder()
            .fullName("John Doe")
            .email(user.getEmail())
            .profileImage(null)
            .build();

        AuthResponse expectedResponse = AuthResponse.builder()
            .accessToken("access-token")
            .refreshToken("refresh-token")
            .expiresIn(3600L)
            .profile(profile)
            .build();
        when(tokenService.buildAuthResponse(user)).thenReturn(expectedResponse);

        AuthResponse result = userService.login(request);

        assertNotNull(result);
        assertEquals("access-token", result.getAccessToken());
        assertEquals("refresh-token", result.getRefreshToken());
        assertEquals(3600L, result.getExpiresIn());
        assertNotNull(result.getProfile());
        assertEquals(user.getEmail(), result.getProfile().getEmail());

        verify(passwordEncoder).matches(request.getPassword(), user.getPassword());
        verify(tokenService).buildAuthResponse(user);
    }

    // User not found for given email should throw UserNotFoundException
    @Test
    void login_whenUserNotFound_throwsUserNotFoundException() {
        LoginRequest request = LoginRequest.builder()
                .email("missing@example.com")
                .password("P@ssword123")
                .build();

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.login(request));

        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(tokenService, never()).buildAuthResponse(any());
    }

    // Inactive user should not be able to login
    @Test
    void login_whenUserInactive_throwsInvalidCredentialsException() {
        LoginRequest request = LoginRequest.builder()
                .email("inactive@example.com")
                .password("P@ssword123")
                .build();

        User user = new User();
        user.setUserId(20);
        user.setEmail(request.getEmail());
        user.setPassword("hashed");
        user.setStatus(UserStatus.SUSPENDED);

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));

        InvalidCredentialsException ex = assertThrows(InvalidCredentialsException.class,
                     () -> userService.login(request));
        assertEquals("User account is not active", ex.getMessage());

        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(tokenService, never()).buildAuthResponse(any());
    }

    // Users without a stored password must use social login instead
    @Test
    void login_whenPasswordMissing_throwsInvalidCredentialsExceptionForSocialLogin() {
        LoginRequest request = LoginRequest.builder()
                .email("social@example.com")
                .password("P@ssword123")
                .build();

        User user = new User();
        user.setUserId(30);
        user.setEmail(request.getEmail());
        user.setPassword(null); // no local password
        user.setStatus(UserStatus.ACTIVE);

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));

        InvalidCredentialsException ex = assertThrows(InvalidCredentialsException.class, 
                () -> userService.login(request));
        assertEquals("Use social login for this account", ex.getMessage());

        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(tokenService, never()).buildAuthResponse(any());
    }

    // Wrong password should throw InvalidCredentialsException
    @Test
    void login_whenPasswordInvalid_throwsInvalidCredentialsException() {
        LoginRequest request = LoginRequest.builder()
                .email("john.doe@example.com")
                .password("WrongPass123")
                .build();

        User user = new User();
        user.setUserId(40);
        user.setEmail(request.getEmail());
        user.setPassword("hashed-password");
        user.setStatus(UserStatus.ACTIVE);

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.getPassword(), user.getPassword())).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> userService.login(request));

        verify(passwordEncoder).matches(request.getPassword(), user.getPassword());
        verify(tokenService, never()).buildAuthResponse(any());
    }
}
