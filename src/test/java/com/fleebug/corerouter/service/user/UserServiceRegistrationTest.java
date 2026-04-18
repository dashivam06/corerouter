package com.fleebug.corerouter.service.user;

import com.fleebug.corerouter.dto.otp.FinalRegistrationRequest;
import com.fleebug.corerouter.dto.otp.RequestOtpResponse;
import com.fleebug.corerouter.dto.otp.VerifyOtpResponse;
import com.fleebug.corerouter.dto.user.response.AuthResponse;
import com.fleebug.corerouter.entity.user.User;
import com.fleebug.corerouter.enums.otp.OtpPurpose;
import com.fleebug.corerouter.enums.user.UserStatus;
import com.fleebug.corerouter.exception.user.InvalidOtpException;
import com.fleebug.corerouter.exception.user.UserAlreadyExistsException;
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
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the 3-step email+OTP user registration flow.
 */
class UserServiceRegistrationTest {

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

    // requestOtp should reject already-registered email
    @Test
    void requestOtp_whenEmailAlreadyExists_throwsUserAlreadyExistsException() {
        String email = "existing@example.com";
        when(userRepository.existsByEmail(email)).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () ->
                userService.requestOtp(email, OtpPurpose.REGISTRATION)
        );

        verify(otpService, never()).requestOtp(anyString(), any(), any(), any());
    }

    // requestOtp should call OtpService and build expected response for new email
    @Test
    void requestOtp_whenEmailNew_sendsOtpAndReturnsResponse() {
        String email = "new@example.com";
        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(otpService.requestOtp(email, OtpPurpose.REGISTRATION, null, null))
                    .thenReturn("ver-reg-1");

        RequestOtpResponse response = userService.requestOtp(email, OtpPurpose.REGISTRATION);

        assertNotNull(response);
        assertEquals("ver-reg-1", response.getVerificationId());
        assertEquals("OTP sent to " + email, response.getMessage());
        assertEquals(5, response.getTtlMinutes());

        verify(otpService).requestOtp(email, OtpPurpose.REGISTRATION, null, null);
    }

    // Step 2: verifyOtp should delegate to OtpService and return a verified response
    @Test
    void verifyOtp_whenOtpValid_returnsVerifiedResponse() {
        String verificationId = "ver-reg-2";
        String otp = "123456";

        // Successful validation is represented by no exception
        userService.verifyOtp(verificationId, otp);

        verify(otpService).validateOtp(verificationId, otp);
        VerifyOtpResponse response = userService.verifyOtp(verificationId, otp);

        assertNotNull(response);
        assertEquals(verificationId, response.getVerificationId());
        assertTrue(response.isVerified());
        assertEquals(20, response.getProfileCompletionTtlMinutes());
    }

    // FinalRegister should fail if verificationId is not marked verified
    @Test
    void finalRegister_whenNotVerified_throwsInvalidOtpException() {
        String verificationId = "ver-reg-3";
        FinalRegistrationRequest request = new FinalRegistrationRequest();
        request.setPassword("Secure#123");

        when(otpService.isVerified(verificationId)).thenReturn(false);

        assertThrows(InvalidOtpException.class, () ->
                userService.finalRegister(verificationId, request)
        );

        verify(otpService).isVerified(verificationId);
        verify(otpService, never()).getEmail(anyString());
        verifyNoInteractions(userRepository, passwordEncoder, tokenService);
    }

    // Final User Creation method should hash password, save ACTIVE user, cleanup verification and build tokens
    @Test
    void finalRegister_whenVerified_createsUserAndReturnsAuthResponse() {
        String verificationId = "ver-reg-4";
        String email = "user@example.com";
        String plainPassword = "Secure#123";
        String encodedPassword = "{bcrypt}encoded";

        FinalRegistrationRequest request = new FinalRegistrationRequest();
        request.setFullName("New User");
        request.setPassword(plainPassword);
        request.setConfirmPassword(plainPassword);
        request.setProfileImage("https://example.com/avatar.png");
        request.setEmailSubscribed(true);

        when(otpService.isVerified(verificationId)).thenReturn(true);
        when(otpService.getEmail(verificationId)).thenReturn(email);
        when(passwordEncoder.encode(plainPassword)).thenReturn(encodedPassword);

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setUserId(100);
            return u;
        });

        AuthResponse authResponse = new AuthResponse();
        authResponse.setAccessToken("access-token");
        when(tokenService.buildAuthResponse(any(User.class))).thenReturn(authResponse);

        AuthResponse result = userService.finalRegister(verificationId, request);

        assertNotNull(result);
        assertEquals("access-token", result.getAccessToken());

        // ensure user persisted with encoded password and ACTIVE status
        verify(userRepository).save(argThat(user ->
                email.equals(user.getEmail()) &&
                        encodedPassword.equals(user.getPassword()) &&
                        user.getStatus() == UserStatus.ACTIVE &&
                        "New User".equals(user.getFullName())
        ));

        verify(otpService).cleanupVerification(verificationId);
        verify(tokenService).buildAuthResponse(any(User.class));
    }
}
