package com.hus.mim_backend.application.rbac.model;

/**
 * Flat row for role-permission matrix query.
 */
public class RolePermissionRow {
    private String roleName;
    private String roleDescription;
    private String permissionName;
    private String permissionDescription;
    private String permissionResource;
    private String permissionAction;

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getRoleDescription() {
        return roleDescription;
    }

    public void setRoleDescription(String roleDescription) {
        this.roleDescription = roleDescription;
    }

    public String getPermissionName() {
        return permissionName;
    }

    public void setPermissionName(String permissionName) {
        this.permissionName = permissionName;
    }

    public String getPermissionDescription() {
        return permissionDescription;
    }

    public void setPermissionDescription(String permissionDescription) {
        this.permissionDescription = permissionDescription;
    }

    public String getPermissionResource() {
        return permissionResource;
    }

    public void setPermissionResource(String permissionResource) {
        this.permissionResource = permissionResource;
    }

    public String getPermissionAction() {
        return permissionAction;
    }

    public void setPermissionAction(String permissionAction) {
        this.permissionAction = permissionAction;
    }
}
