package com.hus.mim_backend.application.rbac.model;

import lombok.Getter;
import lombok.Setter;

/**
 * Per-user permission override.
 */
@Setter
@Getter
public class UserPermissionOverride {
    private String permission;
    private String effect;

}
