package com.hus.mim_backend.infrastructure.adapter.web.admin;

import com.hus.mim_backend.application.rbac.dto.PermissionDefinitionResponse;
import com.hus.mim_backend.application.rbac.dto.RolePermissionResponse;
import com.hus.mim_backend.application.rbac.dto.UpdateUserPermissionOverridesRequest;
import com.hus.mim_backend.application.rbac.dto.UpdateUserRoleRequest;
import com.hus.mim_backend.application.rbac.dto.UserRbacAssignmentResponse;
import com.hus.mim_backend.application.rbac.usecase.ManageRbacUseCase;
import com.hus.mim_backend.domain.shared.DomainException;
import com.hus.mim_backend.shared.api.ApiResponse;
import com.hus.mim_backend.shared.constants.ApiEndpoints;
import com.hus.mim_backend.shared.constants.RbacPermissions;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Web adapter for RBAC administration APIs.
 */
@RestController
@RequestMapping(ApiEndpoints.ADMIN + ApiEndpoints.RBAC)
@PreAuthorize("hasAuthority('PERM_" + RbacPermissions.RBAC_MANAGE + "')")
public class AdminRbacController {

    private final ManageRbacUseCase manageRbacUseCase;

    public AdminRbacController(ManageRbacUseCase manageRbacUseCase) {
        this.manageRbacUseCase = manageRbacUseCase;
    }

    @GetMapping(ApiEndpoints.RBAC_PERMISSIONS)
    public ResponseEntity<ApiResponse<List<PermissionDefinitionResponse>>> getDelegablePermissions() {
        return ResponseEntity.ok(ApiResponse.success(
                manageRbacUseCase.getDelegablePermissions(),
                "Get delegable permissions successfully"));
    }

    @GetMapping(ApiEndpoints.RBAC_ROLES)
    public ResponseEntity<ApiResponse<List<RolePermissionResponse>>> getRolePermissionMatrix() {
        return ResponseEntity.ok(ApiResponse.success(
                manageRbacUseCase.getRolePermissionMatrix(),
                "Get role permission matrix successfully"));
    }

    @GetMapping(ApiEndpoints.RBAC_USERS)
    public ResponseEntity<ApiResponse<List<UserRbacAssignmentResponse>>> getUserAssignments() {
        return ResponseEntity.ok(ApiResponse.success(
                manageRbacUseCase.getUserAssignments(),
                "Get RBAC user assignments successfully"));
    }

    @PutMapping(ApiEndpoints.RBAC_USER_OVERRIDES)
    public ResponseEntity<ApiResponse<UserRbacAssignmentResponse>> updateUserOverrides(
            @PathVariable UUID userId,
            @RequestBody UpdateUserPermissionOverridesRequest request) {
        try {
            UserRbacAssignmentResponse response = manageRbacUseCase.updateUserOverrides(userId, request);
            return ResponseEntity.ok(ApiResponse.success(response, "Update RBAC overrides successfully"));
        } catch (DomainException ex) {
            if ("User not found".equalsIgnoreCase(ex.getMessage())) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("User not found", "USER_NOT_FOUND"));
            }
            throw ex;
        }
    }

    @PatchMapping(ApiEndpoints.RBAC_USER_ROLES)
    public ResponseEntity<ApiResponse<UserRbacAssignmentResponse>> updateUserRole(
            @PathVariable UUID userId,
            @RequestBody UpdateUserRoleRequest request,
            Authentication authentication) {
        String actorEmail = resolveAuthenticatedEmail(authentication);
        try {
            UserRbacAssignmentResponse response = manageRbacUseCase.updateUserRole(actorEmail, userId, request);
            return ResponseEntity.ok(ApiResponse.success(response, "Update RBAC role successfully"));
        } catch (DomainException ex) {
            if ("User not found".equalsIgnoreCase(ex.getMessage())) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("User not found", "USER_NOT_FOUND"));
            }
            if (StringUtils.hasText(ex.getMessage()) && ex.getMessage().startsWith("Role not found:")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error(ex.getMessage(), "ROLE_NOT_FOUND"));
            }
            throw ex;
        }
    }

    private String resolveAuthenticatedEmail(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new DomainException("Authentication required");
        }
        String email = String.valueOf(authentication.getPrincipal());
        if (!StringUtils.hasText(email)) {
            throw new DomainException("Authentication required");
        }
        return email;
    }
}
