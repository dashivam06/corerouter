package com.fleebug.corerouter.controller.user;

import com.fleebug.corerouter.dto.common.ApiResponse;
import com.fleebug.corerouter.dto.user.request.ChangePasswordRequest;
import com.fleebug.corerouter.dto.user.request.DeleteAccountRequest;
import com.fleebug.corerouter.dto.user.request.UpdateProfileRequest;
import com.fleebug.corerouter.dto.user.response.UserProfileResponse;
import com.fleebug.corerouter.entity.user.User;
import com.fleebug.corerouter.security.details.CustomUserDetails;
import com.fleebug.corerouter.service.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/user/profile")
@RequiredArgsConstructor
@Tag(name = "User Profile", description = "Authenticated user profile operations")
public class UserProfileController {

    private final UserService userService;

    @Operation(summary = "Get my profile", description = "Fetch authenticated user's profile information")
    @GetMapping
    public ResponseEntity<ApiResponse<UserProfileResponse>> getMyProfile(
            Authentication authentication,
            HttpServletRequest request) {
        User user = ((CustomUserDetails) authentication.getPrincipal()).getUser();
        UserProfileResponse response = userService.getMyProfile(user.getUserId());

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Profile fetched successfully", response, request));
    }

    @Operation(summary = "Update my profile", description = "Update authenticated user's name and profile image")
    @PatchMapping
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest updateRequest,
            HttpServletRequest request) {
        User user = ((CustomUserDetails) authentication.getPrincipal()).getUser();
        UserProfileResponse response = userService.updateProfile(user.getUserId(), updateRequest);

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Profile updated successfully", response, request));
    }

    @Operation(summary = "Change my password", description = "Change authenticated user's password")
    @PatchMapping("/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest changePasswordRequest,
            HttpServletRequest request) {
        User user = ((CustomUserDetails) authentication.getPrincipal()).getUser();
        userService.changePassword(user.getUserId(), changePasswordRequest);

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Password changed successfully", null, request));
    }

    @Operation(summary = "Delete my account", description = "Soft delete authenticated user's account while preserving historical records")
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> deleteAccount(
            Authentication authentication,
            @Valid @RequestBody DeleteAccountRequest deleteAccountRequest,
            HttpServletRequest request) {
        User user = ((CustomUserDetails) authentication.getPrincipal()).getUser();
        userService.softDeleteAccount(user.getUserId(), deleteAccountRequest.getPassword());

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Account deleted successfully", null, request));
    }
}
