package com.hus.mim_backend.infrastructure.adapter.web.admin;

import com.hus.mim_backend.application.auth.dto.RegisterRequest;
import com.hus.mim_backend.application.auth.dto.UserResponse;
import com.hus.mim_backend.application.auth.usecase.AdminProvisionUserUseCase;
import com.hus.mim_backend.shared.api.ApiResponse;
import com.hus.mim_backend.shared.constants.ApiEndpoints;
import com.hus.mim_backend.shared.constants.RbacPermissions;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
