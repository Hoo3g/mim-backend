package com.hus.mim_backend.application.rbac.model;

import lombok.Getter;
import lombok.Setter;

/**
 * Flat row for role-permission matrix query.
 */
@Setter
@Getter
public class RolePermissionRow {
    private String roleName;
    private String roleDescription;
    private String permissionName;
    private String permissionDescription;
    private String permissionResource;
    private String permissionAction;

}
