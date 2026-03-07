package com.hus.mim_backend.application.port.output;

import com.hus.mim_backend.application.rbac.model.PermissionDefinition;
import com.hus.mim_backend.application.rbac.model.RolePermissionRow;
import com.hus.mim_backend.application.rbac.model.UserPermissionOverride;
import com.hus.mim_backend.application.rbac.model.UserRbacAssignment;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Output port for RBAC read/write operations.
 */
public interface RbacRepository {
    Set<String> findRolesByEmail(String email);

    Set<String> findEffectivePermissionsByEmail(String email);

    Set<String> findEffectivePermissionsByUserId(UUID userId);

    List<PermissionDefinition> findPermissionCatalog();

    List<RolePermissionRow> findRolePermissionRows();

    List<UserRbacAssignment> findUserAssignments();

    Optional<UserRbacAssignment> findUserAssignmentById(UUID userId);

    List<UserPermissionOverride> findUserOverrides(UUID userId);

    Optional<UUID> findUserIdByEmail(String email);

    boolean existsUser(UUID userId);

    boolean isAdminUser(UUID userId);

    boolean existsRole(String roleName);

    void grantRole(UUID userId, String roleName);

    int revokeRole(UUID userId, String roleName);

    void replaceOverrides(UUID userId, Set<String> grants, Set<String> denies, Set<String> managedPermissionNames);
}
