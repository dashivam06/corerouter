package com.fleebug.corerouter.service.user;

import com.fleebug.corerouter.dto.user.request.UpdateProfileRequest;
import com.fleebug.corerouter.dto.user.response.UserProfileResponse;
import com.fleebug.corerouter.entity.user.User;
import com.fleebug.corerouter.enums.user.UserRole;
import com.fleebug.corerouter.enums.user.UserStatus;
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
 * Unit tests for user profile updates.
 */
class UserServiceProfileUpdateTest {

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
                .fullName("Original Name")
                .status(UserStatus.ACTIVE)
                .role(UserRole.USER)
                .build();
    }

    @Test
    // Tests that attempting to update a non-existent user throws UserNotFoundException
    void updateProfile_whenUserNotFound_throwsUserNotFoundException() {
        when(userRepository.findById(999)).thenReturn(Optional.empty());

        UpdateProfileRequest request = new UpdateProfileRequest();
        
        assertThrows(UserNotFoundException.class, () ->
                userService.updateProfile(999, request, "127.0.0.1")
        );
    }

    @Test
    // Tests that updating a deleted account throws IllegalArgumentException
    void updateProfile_whenUserDeleted_throwsIllegalArgumentException() {
        activeUser.setStatus(UserStatus.DELETED);
        when(userRepository.findById(1)).thenReturn(Optional.of(activeUser));

        UpdateProfileRequest request = new UpdateProfileRequest();
        
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                userService.updateProfile(1, request, "127.0.0.1")
        );

        assertEquals("Deleted account cannot be updated", ex.getMessage());
    }

    @Test
    // Tests that explicitly provided profile fields are updated and saved correctly
    void updateProfile_whenValidRequest_updatesFieldsAndReturnsResponse() {
        when(userRepository.findById(1)).thenReturn(Optional.of(activeUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFullName("Updated Name  ");
        request.setProfileImage("  https://example.com/image.png  ");
        request.setEmailSubscribed(false);

        UserProfileResponse response = userService.updateProfile(1, request, "127.0.0.1");

        assertNotNull(response);
        assertEquals("Updated Name", response.getFullName());
        assertEquals("https://example.com/image.png", response.getProfileImage());
        assertEquals(false, response.getEmailSubscribed());

        verify(userRepository).save(activeUser);
    }

    @Test
    // Tests that if fields are null or empty strings, they do not overwrite existing valid data or are handled properly
    void updateProfile_whenBlankFields_ignoresBlankFieldsAndTrims() {
        activeUser.setProfileImage("original.png");
        when(userRepository.findById(1)).thenReturn(Optional.of(activeUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFullName("   "); // Blank name should be ignored
        request.setProfileImage(" "); // Blank image should become null

        UserProfileResponse response = userService.updateProfile(1, request, "127.0.0.1");

        assertEquals("Original Name", response.getFullName()); // Unchanged
        assertNull(response.getProfileImage()); // Set to null since it was empty
        
        verify(userRepository).save(activeUser);
    }
}
