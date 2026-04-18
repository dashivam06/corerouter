package com.fleebug.corerouter.service.user;

import com.fleebug.corerouter.dto.otp.RequestOtpResponse;
import com.fleebug.corerouter.dto.otp.VerifyOtpResponse;
import com.fleebug.corerouter.entity.user.User;
import com.fleebug.corerouter.enums.otp.OtpPurpose;
import com.fleebug.corerouter.exception.user.InvalidOtpException;
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
 * Unit tests for the forgot password flow (OTP request, verify, reset).
 */
class UserServiceForgotPasswordTest {

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

    // Forgot password: requesting OTP for existing user should call OtpService and return response
    @Test
    void requestPasswordResetOtp_whenUserExists_sendsOtpAndReturnsResponse() {
        String email = "reset@example.com";
        User user = new User();
        user.setUserId(123);
        user.setEmail(email);
        user.setFullName("Reset User");

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(otpService.requestOtp(email, OtpPurpose.PASSWORD_RESET, user.getFullName(), user.getUserId()))
                .thenReturn("ver-otp-1");

        RequestOtpResponse response = userService.requestPasswordResetOtp(email);

        assertNotNull(response);
        assertEquals("ver-otp-1", response.getVerificationId());
        assertEquals("OTP sent to " + email, response.getMessage());
        assertEquals(5, response.getTtlMinutes());

        verify(otpService).requestOtp(email, OtpPurpose.PASSWORD_RESET, user.getFullName(), user.getUserId());
    }

    // Forgot password: requesting OTP for unknown email should throw UserNotFoundException
    @Test
    void requestPasswordResetOtp_whenUserNotFound_throwsUserNotFoundException() {
        String email = "unknown@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.requestPasswordResetOtp(email));

        verify(otpService, never()).requestOtp(anyString(), any(), any(), any());
    }

    // Forgot password: verifying OTP should delegate to OtpService and build expected response
    @Test
    void verifyPasswordResetOtp_whenOtpValid_returnsExpectedResponse() {
        String verificationId = "ver-otp-2";
        String otp = "123456";

        VerifyOtpResponse response = userService.verifyPasswordResetOtp(verificationId, otp);

        verify(otpService).validateOtp(verificationId, otp);

        assertNotNull(response);
        assertEquals(verificationId, response.getVerificationId());
        assertTrue(response.isVerified());
        assertEquals(20, response.getProfileCompletionTtlMinutes());
    }

    // Reset password: new and confirm mismatch should fail fast
    @Test
    void resetPasswordWithVerification_whenPasswordsDoNotMatch_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                userService.resetPasswordWithVerification("ver-1", 
                "NewPass#1", "Different#2")
        );

        verifyNoInteractions(otpService, userRepository, passwordEncoder);
    }

    // Reset password: unverified OTP should not allow reset
    @Test
    void resetPasswordWithVerification_whenNotVerified_throwsInvalidOtpException() {
        String verificationId = "ver-2";

        when(otpService.isVerified(verificationId)).thenReturn(false);

        assertThrows(InvalidOtpException.class, () ->
                userService.resetPasswordWithVerification(verificationId, "NewPass#1", 
                "NewPass#1")
        );

        verify(otpService).isVerified(verificationId);
        verify(otpService, never()).getEmail(anyString());
        verifyNoInteractions(userRepository, passwordEncoder);
    }

    // Reset password: missing email for verified session should be treated as expired
    @Test
    void resetPasswordWithVerification_whenEmailMissing_throwsInvalidOtpException() {
        String verificationId = "ver-3";

        when(otpService.isVerified(verificationId)).thenReturn(true);
        when(otpService.getEmail(verificationId)).thenReturn(null);

        InvalidOtpException ex = assertThrows(InvalidOtpException.class, () ->
                userService.resetPasswordWithVerification(verificationId, 
                    "NewPass#1", "NewPass#1")
        );

        assertEquals("Verification session expired. Please request OTP again.", ex.getMessage());
        verify(otpService).isVerified(verificationId);
        verify(otpService).getEmail(verificationId);
        verifyNoInteractions(userRepository, passwordEncoder);
    }

    // Reset password: unknown email despite verified session should throw UserNotFoundException
    @Test
    void resetPasswordWithVerification_whenUserNotFound_throwsUserNotFoundException() {
        String verificationId = "ver-4";
        String email = "no-user@example.com";

        when(otpService.isVerified(verificationId)).thenReturn(true);
        when(otpService.getEmail(verificationId)).thenReturn(email);
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () ->
                userService.resetPasswordWithVerification(verificationId, 
                    "NewPass#1", "NewPass#1")
        );

        verify(userRepository).findByEmail(email);
        verify(passwordEncoder, never()).encode(anyString());
    }

    // Reset password: happy path should encode password, save user, and publish notifications
    @Test
    void resetPasswordWithVerification_whenAllValid_updatesPasswordAndCleansUp() {
        String verificationId = "ver-5";
        String email = "user@example.com";
        String newPassword = "NewPass#1";
        String encodedPassword = "{bcrypt}encoded";

        User user = new User();
        user.setUserId(99);
        user.setEmail(email);
        user.setFullName("Test User");

        when(otpService.isVerified(verificationId)).thenReturn(true);
        when(otpService.getEmail(verificationId)).thenReturn(email);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(newPassword)).thenReturn(encodedPassword);

        userService.resetPasswordWithVerification(verificationId, newPassword, newPassword);

        verify(passwordEncoder).encode(newPassword);
        verify(userRepository).save(argThat(saved -> encodedPassword.equals(saved.getPassword())));
        verify(otpService).publishPasswordChangedNotification(email, user.getFullName(), user.getUserId());
        verify(otpService).cleanupVerification(verificationId);
    }
}
