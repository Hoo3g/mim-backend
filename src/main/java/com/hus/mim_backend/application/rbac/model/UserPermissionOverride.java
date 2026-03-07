package com.hus.mim_backend.application.rbac.model;

/**
 * Per-user permission override.
 */
public class UserPermissionOverride {
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
