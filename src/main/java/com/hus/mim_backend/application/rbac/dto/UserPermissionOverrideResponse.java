package com.hus.mim_backend.application.rbac.dto;

/**
 * User-specific override response DTO.
 */
public class UserPermissionOverrideResponse {
    private String permission;
    private String effect;

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public String getEffect() {
        return effect;
    }

    public void setEffect(String effect) {
        this.effect = effect;
    }
}
