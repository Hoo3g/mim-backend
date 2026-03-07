package com.hus.mim_backend.application.rbac.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * User RBAC assignment response DTO.
 */
public class UserRbacAssignmentResponse {
    private UUID userId;
    private String displayName;
    private String email;
    private String accountStatus;
    private List<String> roles;
    private List<String> effectivePermissions;
    private List<UserPermissionOverrideResponse> overrides;
    private LocalDateTime createdAt;

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAccountStatus() {
        return accountStatus;
    }

    public void setAccountStatus(String accountStatus) {
        this.accountStatus = accountStatus;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public List<String> getEffectivePermissions() {
        return effectivePermissions;
    }

    public void setEffectivePermissions(List<String> effectivePermissions) {
        this.effectivePermissions = effectivePermissions;
    }

    public List<UserPermissionOverrideResponse> getOverrides() {
        return overrides;
    }

    public void setOverrides(List<UserPermissionOverrideResponse> overrides) {
        this.overrides = overrides;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
