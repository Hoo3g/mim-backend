package com.hus.mim_backend.infrastructure.adapter.web.admin;

import com.hus.mim_backend.application.auth.dto.RegisterRequest;
import com.hus.mim_backend.application.auth.dto.UserResponse;
import com.hus.mim_backend.application.auth.usecase.AdminProvisionUserUseCase;
import com.hus.mim_backend.shared.api.ApiResponse;
import com.hus.mim_backend.shared.constants.ApiEndpoints;
import com.hus.mim_backend.shared.constants.RbacPermissions;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping(ApiEndpoints.ADMIN_USERS)
@PreAuthorize("hasAuthority('PERM_" + RbacPermissions.RBAC_MANAGE + "') or hasRole('ADMIN')")
public class AdminUserController {

    private final AdminProvisionUserUseCase adminProvisionUserUseCase;

    public AdminUserController(AdminProvisionUserUseCase adminProvisionUserUseCase) {
        this.adminProvisionUserUseCase = adminProvisionUserUseCase;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> createUser(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                adminProvisionUserUseCase.createUserByAdmin(request),
                "User account created successfully"));
    }

    @PatchMapping(ApiEndpoints.ADMIN_USER_LOCK)
    public ResponseEntity<ApiResponse<UserResponse>> lockUser(
            @PathVariable UUID userId,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                adminProvisionUserUseCase.lockUserByAdmin(resolveAuthenticatedEmail(authentication), userId),
                "User account locked successfully"));
    }

    @PatchMapping(ApiEndpoints.ADMIN_USER_UNLOCK)
    public ResponseEntity<ApiResponse<UserResponse>> unlockUser(
            @PathVariable UUID userId,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                adminProvisionUserUseCase.unlockUserByAdmin(resolveAuthenticatedEmail(authentication), userId),
                "User account unlocked successfully"));
    }

    @DeleteMapping(ApiEndpoints.ADMIN_USER_BY_ID)
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @PathVariable UUID userId,
            Authentication authentication) {
        adminProvisionUserUseCase.deleteUserByAdmin(resolveAuthenticatedEmail(authentication), userId);
        return ResponseEntity.ok(ApiResponse.success(null, "User account deleted successfully"));
    }

    private String resolveAuthenticatedEmail(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("Authentication required");
        }
        String email = String.valueOf(authentication.getPrincipal());
        if (!StringUtils.hasText(email)) {
            throw new IllegalStateException("Authentication required");
        }
        return email;
    }
}
