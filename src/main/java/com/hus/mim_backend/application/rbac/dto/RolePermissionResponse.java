package com.hus.mim_backend.application.rbac.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Role -> permissions response DTO.
 */
@Setter
@Getter
public class RolePermissionResponse {
    private String role;
    private String description;
    private List<PermissionDefinitionResponse> permissions;

}
