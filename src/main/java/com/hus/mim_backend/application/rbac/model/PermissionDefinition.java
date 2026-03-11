package com.hus.mim_backend.application.rbac.model;

import lombok.Getter;
import lombok.Setter;

/**
 * Permission definition model from RBAC catalog.
 */
@Setter
@Getter
public class PermissionDefinition {
    private String name;
    private String description;
    private String resource;
    private String action;

}
