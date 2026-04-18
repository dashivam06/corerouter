package com.fleebug.corerouter.service.user;

import com.fleebug.corerouter.dto.user.response.PaginatedUserListResponse;
import com.fleebug.corerouter.dto.user.response.UserProfileResponse;
import com.fleebug.corerouter.entity.user.User;
import com.fleebug.corerouter.enums.user.UserRole;
import com.fleebug.corerouter.enums.user.UserStatus;
import com.fleebug.corerouter.repository.user.UserRepository;
import com.fleebug.corerouter.service.otp.OtpService;
import com.microsoft.applicationinsights.TelemetryClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceAdminTest {

    @Mock private UserRepository userRepository;
    @Mock private TelemetryClient telemetryClient;
    @Mock private OtpService otpService;

    @InjectMocks
    private UserService userService;

    private User targetUser;

    @BeforeEach
    void setUp() {
        targetUser = User.builder()
                .userId(1)
                .email("user@example.com")
                .fullName("Test User")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();
    }

    @Test
    // Tests that an admin can successfully change a user's role
    void updateUserRoleByAdmin_whenValid_updatesRoleSuccessfully() {
        when(userRepository.findById(1)).thenReturn(Optional.of(targetUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserProfileResponse response = userService.updateUserRoleByAdmin(1, UserRole.ADMIN);

        assertNotNull(response);
        assertEquals(1, response.getUserId());
        assertEquals(UserRole.ADMIN, targetUser.getRole());
        verify(userRepository).save(targetUser);
    }

    @Test
    // Tests that an admin changing a role to the same existing role throws an exception
    void updateUserRoleByAdmin_whenSameRole_throwsIllegalArgumentException() {
        when(userRepository.findById(1)).thenReturn(Optional.of(targetUser));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
            userService.updateUserRoleByAdmin(1, UserRole.USER)
        );
        assertEquals("User already has role USER", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    // Tests that an admin can successfully update a user's status to SUSPENDED
    void updateUserStatusByAdmin_whenSuspended_updatesStatus() {
        when(userRepository.findById(1)).thenReturn(Optional.of(targetUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserProfileResponse response = userService.updateUserStatusByAdmin(1, UserStatus.SUSPENDED);

        assertNotNull(response);
        assertEquals(UserStatus.SUSPENDED, targetUser.getStatus());
        verify(userRepository).save(targetUser);
        verify(otpService, never()).publishUserDeletedNotification(anyString(), anyString(), anyInt(), anyString());
    }

    @Test
    // Tests that changing a status to DELETED triggers the OTP notification
    void updateUserStatusByAdmin_whenDeleted_updatesStatusAndSendsNotification() {
        when(userRepository.findById(1)).thenReturn(Optional.of(targetUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserProfileResponse response = userService.updateUserStatusByAdmin(1, UserStatus.DELETED);

        assertNotNull(response);
        assertEquals(UserStatus.DELETED, targetUser.getStatus());
        assertFalse(targetUser.isEmailSubscribed()); // Deletion un-subscribes users
        verify(otpService).publishUserDeletedNotification("user@example.com", "Test User", 1, "admin");
    }

    @Test
    // Tests filtering and paginating users by both role and status
    void getUsersWithFilters_withRoleAndStatus_returnsPaginatedResponse() {
        User adminUser = User.builder().userId(2).role(UserRole.ADMIN).status(UserStatus.ACTIVE).build();
        Page<User> pageResult = new PageImpl<>(List.of(adminUser), PageRequest.of(0, 10), 1);

        when(userRepository.findByRoleAndStatus(eq(UserRole.ADMIN), eq(UserStatus.ACTIVE), any(Pageable.class)))
                .thenReturn(pageResult);

        PaginatedUserListResponse response = userService.getUsersWithFilters(0, 10, UserRole.ADMIN, UserStatus.ACTIVE);

        assertNotNull(response);
        assertEquals(1, response.getUsers().size());
        assertEquals(2, response.getUsers().get(0).getUserId());
        assertEquals(1, response.getTotalElements());
    }
    
    @Test
    // Tests filtering users by only role
    void getUsersWithFilters_withOnlyRole_returnsFilteredList() {
        Page<User> pageResult = new PageImpl<>(List.of(targetUser), PageRequest.of(0, 10), 1);

        when(userRepository.findByRole(eq(UserRole.USER), any(Pageable.class)))
                .thenReturn(pageResult);

        PaginatedUserListResponse response = userService.getUsersWithFilters(0, 10, UserRole.USER, null);

        assertNotNull(response);
        assertEquals(1, response.getUsers().size());
    }
}
