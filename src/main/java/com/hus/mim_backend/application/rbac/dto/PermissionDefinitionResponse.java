package com.hus.mim_backend.application.rbac.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * RBAC permission response DTO.
 */
@Setter
@Getter
public class PermissionDefinitionResponse {
    private String name;
    private String description;
    private String resource;
    private String action;
    private boolean delegable;

}
