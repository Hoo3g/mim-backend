package com.hus.mim_backend.application.rbac.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * Request DTO for granting/revoking a role on a user.
 */
@Setter
@Getter
public class UpdateUserRoleRequest {
    private String action;
    private String role;

}
