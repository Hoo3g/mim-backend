package com.hus.mim_backend.application.rbac.dto;

/**
 * Request DTO for granting/revoking a role on a user.
 */
public class UpdateUserRoleRequest {
    private String action;
    private String role;

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
