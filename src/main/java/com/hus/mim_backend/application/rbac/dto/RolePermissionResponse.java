package com.hus.mim_backend.application.rbac.dto;

import java.util.List;

/**
 * Role -> permissions response DTO.
 */
public class RolePermissionResponse {
    private String role;
    private String description;
    private List<PermissionDefinitionResponse> permissions;

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<PermissionDefinitionResponse> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<PermissionDefinitionResponse> permissions) {
        this.permissions = permissions;
    }
}
