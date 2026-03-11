package com.hus.mim_backend.application.rbac.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * User-specific override response DTO.
 */
@Setter
@Getter
public class UserPermissionOverrideResponse {
    private String permission;
    private String effect;

}
