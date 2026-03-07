package com.hus.mim_backend.application.rbac.service;

import com.hus.mim_backend.application.port.output.RbacRepository;
import com.hus.mim_backend.application.rbac.dto.PermissionDefinitionResponse;
import com.hus.mim_backend.application.rbac.dto.RolePermissionResponse;
import com.hus.mim_backend.application.rbac.dto.UpdateUserPermissionOverridesRequest;
import com.hus.mim_backend.application.rbac.dto.UpdateUserRoleRequest;
import com.hus.mim_backend.application.rbac.dto.UserPermissionOverrideResponse;
import com.hus.mim_backend.application.rbac.dto.UserRbacAssignmentResponse;
import com.hus.mim_backend.application.rbac.model.PermissionDefinition;
import com.hus.mim_backend.application.rbac.model.RolePermissionRow;
import com.hus.mim_backend.application.rbac.model.UserPermissionOverride;
import com.hus.mim_backend.application.rbac.model.UserRbacAssignment;
import com.hus.mim_backend.application.rbac.usecase.ManageRbacUseCase;
import com.hus.mim_backend.domain.shared.DomainException;
import com.hus.mim_backend.shared.constants.RbacPermissions;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Application service for RBAC use cases.
 */
public class RbacServiceImpl implements ManageRbacUseCase {
    private static final String ROLE_ADMIN = "ADMIN";
    private static final Set<String> DELEGABLE_PERMISSIONS = Set.of(
            RbacPermissions.ADMIN_DASHBOARD_VIEW,
            RbacPermissions.MODERATION_POSTS_VIEW,
            RbacPermissions.MODERATION_POSTS_ACTION,
            RbacPermissions.MODERATION_PAPERS_VIEW,
            RbacPermissions.MODERATION_PAPERS_ACTION,
            RbacPermissions.RESEARCH_HERO_EDIT);

    private final RbacRepository rbacRepository;

    public RbacServiceImpl(RbacRepository rbacRepository) {
        this.rbacRepository = rbacRepository;
    }

    @Override
    public Set<String> getRolesByEmail(String email) {
        return rbacRepository.findRolesByEmail(email);
    }

    @Override
    public Set<String> getEffectivePermissionsByEmail(String email) {
        return rbacRepository.findEffectivePermissionsByEmail(email);
    }

    @Override
    public Set<String> getEffectivePermissionsByUserId(UUID userId) {
        return rbacRepository.findEffectivePermissionsByUserId(userId);
    }

    @Override
    public List<PermissionDefinitionResponse> getDelegablePermissions() {
        return rbacRepository.findPermissionCatalog().stream()
                .filter(permission -> DELEGABLE_PERMISSIONS.contains(permission.getName()))
                .map(this::toPermissionDefinitionResponse)
                .toList();
    }

    @Override
    public List<RolePermissionResponse> getRolePermissionMatrix() {
        Map<String, RolePermissionResponse> grouped = new LinkedHashMap<>();
        for (RolePermissionRow row : rbacRepository.findRolePermissionRows()) {
            RolePermissionResponse role = grouped.computeIfAbsent(row.getRoleName(), key -> {
                RolePermissionResponse item = new RolePermissionResponse();
                item.setRole(key);
                item.setDescription(row.getRoleDescription());
                item.setPermissions(new ArrayList<>());
                return item;
            });

            if (!StringUtils.hasText(row.getPermissionName())) {
                continue;
            }

            PermissionDefinition permission = new PermissionDefinition();
            permission.setName(row.getPermissionName());
            permission.setDescription(row.getPermissionDescription());
            permission.setResource(row.getPermissionResource());
            permission.setAction(row.getPermissionAction());
            role.getPermissions().add(toPermissionDefinitionResponse(permission));
        }
        return new ArrayList<>(grouped.values());
    }

    @Override
    public List<UserRbacAssignmentResponse> getUserAssignments() {
        return rbacRepository.findUserAssignments().stream()
                .filter(item -> item.getRoles().stream().noneMatch(role -> "ADMIN".equalsIgnoreCase(role)))
                .map(this::buildUserAssignmentResponse)
                .toList();
    }

    @Override
    public UserRbacAssignmentResponse updateUserOverrides(UUID userId, UpdateUserPermissionOverridesRequest request) {
        if (userId == null) {
            throw new DomainException("userId is required");
        }
        if (!rbacRepository.existsUser(userId)) {
            throw new DomainException("User not found");
        }
        if (rbacRepository.isAdminUser(userId)) {
            throw new DomainException("Only lower accounts can be delegated by this endpoint");
        }

        Set<String> grants = normalizePermissionSet(request == null ? null : request.getGrants());
        Set<String> denies = normalizePermissionSet(request == null ? null : request.getDenies());
        ensureNoOverlap(grants, denies);
        ensureDelegable(grants, "grants");
        ensureDelegable(denies, "denies");

        rbacRepository.replaceOverrides(userId, grants, denies, DELEGABLE_PERMISSIONS);

        UserRbacAssignment assignment = rbacRepository.findUserAssignmentById(userId)
                .orElseThrow(() -> new DomainException("User not found"));
        return buildUserAssignmentResponse(assignment);
    }

    @Override
    public UserRbacAssignmentResponse updateUserRole(String actorEmail, UUID userId, UpdateUserRoleRequest request) {
        if (userId == null) {
            throw new DomainException("userId is required");
        }
        if (!rbacRepository.existsUser(userId)) {
            throw new DomainException("User not found");
        }

        RoleAction action = normalizeRoleAction(request);
        String roleName = normalizeRoleName(request);
        if (!rbacRepository.existsRole(roleName)) {
            throw new DomainException("Role not found: " + roleName);
        }

        UserRbacAssignment before = rbacRepository.findUserAssignmentById(userId)
                .orElseThrow(() -> new DomainException("User not found"));

        if (action == RoleAction.GRANT) {
            rbacRepository.grantRole(userId, roleName);
        } else {
            UUID actorId = resolveActorId(actorEmail);
            ensureCanRevokeRole(actorId, userId, roleName, before);
            rbacRepository.revokeRole(userId, roleName);
        }

        UserRbacAssignment after = rbacRepository.findUserAssignmentById(userId)
                .orElseThrow(() -> new DomainException("User not found"));
        return buildUserAssignmentResponse(after);
    }

    private UserRbacAssignmentResponse buildUserAssignmentResponse(UserRbacAssignment assignment) {
        UserRbacAssignmentResponse response = new UserRbacAssignmentResponse();
        response.setUserId(assignment.getUserId());
        response.setDisplayName(assignment.getDisplayName());
        response.setEmail(assignment.getEmail());
        response.setAccountStatus(assignment.getAccountStatus());
        response.setRoles(assignment.getRoles());
        response.setCreatedAt(assignment.getCreatedAt());

        List<UserPermissionOverrideResponse> overrides = rbacRepository.findUserOverrides(assignment.getUserId()).stream()
                .map(this::toOverrideResponse)
                .toList();
        response.setOverrides(overrides);

        List<String> effectivePermissions = rbacRepository.findEffectivePermissionsByUserId(assignment.getUserId()).stream()
                .sorted()
                .toList();
        response.setEffectivePermissions(effectivePermissions);

        return response;
    }

    private UserPermissionOverrideResponse toOverrideResponse(UserPermissionOverride override) {
        UserPermissionOverrideResponse response = new UserPermissionOverrideResponse();
        response.setPermission(override.getPermission());
        response.setEffect(override.getEffect());
        return response;
    }

    private PermissionDefinitionResponse toPermissionDefinitionResponse(PermissionDefinition permission) {
        PermissionDefinitionResponse response = new PermissionDefinitionResponse();
        response.setName(permission.getName());
        response.setDescription(permission.getDescription());
        response.setResource(normalizeScopeValue(permission.getResource()));
        response.setAction(normalizeScopeValue(permission.getAction()));
        response.setDelegable(DELEGABLE_PERMISSIONS.contains(permission.getName()));
        return response;
    }

    private String normalizeScopeValue(String value) {
        if (!StringUtils.hasText(value)) {
            return "UNKNOWN";
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private Set<String> normalizePermissionSet(List<String> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return Set.of();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String permission : permissions) {
            if (!StringUtils.hasText(permission)) {
                continue;
            }
            normalized.add(permission.trim().toUpperCase(Locale.ROOT));
        }
        return normalized;
    }

    private void ensureNoOverlap(Set<String> grants, Set<String> denies) {
        Set<String> overlap = new LinkedHashSet<>(grants);
        overlap.retainAll(denies);
        if (!overlap.isEmpty()) {
            throw new DomainException("A permission cannot exist in both grants and denies: " + overlap);
        }
    }

    private void ensureDelegable(Set<String> permissions, String fieldName) {
        if (permissions.isEmpty()) {
            return;
        }
        List<String> unsupported = permissions.stream()
                .filter(permission -> !DELEGABLE_PERMISSIONS.contains(permission))
                .sorted()
                .toList();
        if (!unsupported.isEmpty()) {
            throw new DomainException(fieldName + " contains unsupported permissions: " + unsupported);
        }
    }

    private RoleAction normalizeRoleAction(UpdateUserRoleRequest request) {
        if (request == null || !StringUtils.hasText(request.getAction())) {
            throw new DomainException("action is required");
        }
        String action = request.getAction().trim().toUpperCase(Locale.ROOT);
        return switch (action) {
            case "GRANT" -> RoleAction.GRANT;
            case "REVOKE" -> RoleAction.REVOKE;
            default -> throw new DomainException("Unsupported action. Use GRANT or REVOKE.");
        };
    }

    private String normalizeRoleName(UpdateUserRoleRequest request) {
        if (request == null || !StringUtils.hasText(request.getRole())) {
            throw new DomainException("role is required");
        }
        return request.getRole().trim().toUpperCase(Locale.ROOT);
    }

    private UUID resolveActorId(String actorEmail) {
        if (!StringUtils.hasText(actorEmail)) {
            throw new DomainException("Authentication required");
        }
        return rbacRepository.findUserIdByEmail(actorEmail.trim())
                .orElseThrow(() -> new DomainException("Authenticated user is not found"));
    }

    private void ensureCanRevokeRole(UUID actorId, UUID targetUserId, String roleName, UserRbacAssignment currentAssignment) {
        if (ROLE_ADMIN.equals(roleName) && actorId.equals(targetUserId)) {
            throw new DomainException("You cannot revoke your own ADMIN role");
        }

        List<String> currentRoles = currentAssignment.getRoles() == null ? List.of() : currentAssignment.getRoles();
        boolean hasTargetRole = currentRoles.stream().anyMatch(role -> roleName.equalsIgnoreCase(role));
        if (!hasTargetRole) {
            return;
        }
        if (currentRoles.size() <= 1) {
            throw new DomainException("User must keep at least one role");
        }
    }

    private enum RoleAction {
        GRANT,
        REVOKE
    }
}
