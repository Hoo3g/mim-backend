package com.hus.mim_backend.application.rbac.usecase;

import com.hus.mim_backend.application.rbac.dto.PermissionDefinitionResponse;
import com.hus.mim_backend.application.rbac.dto.RolePermissionResponse;
import com.hus.mim_backend.application.rbac.dto.UpdateUserPermissionOverridesRequest;
import com.hus.mim_backend.application.rbac.dto.UpdateUserRoleRequest;
import com.hus.mim_backend.application.rbac.dto.UserRbacAssignmentResponse;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Input port for RBAC administration and permission resolution.
 */
public interface ManageRbacUseCase {
    Set<String> getRolesByEmail(String email);

    Set<String> getEffectivePermissionsByEmail(String email);

    Set<String> getEffectivePermissionsByUserId(UUID userId);

    List<PermissionDefinitionResponse> getDelegablePermissions();

    List<RolePermissionResponse> getRolePermissionMatrix();

    List<UserRbacAssignmentResponse> getUserAssignments();

    UserRbacAssignmentResponse updateUserOverrides(UUID userId, UpdateUserPermissionOverridesRequest request);

    UserRbacAssignmentResponse updateUserRole(String actorEmail, UUID userId, UpdateUserRoleRequest request);
}
