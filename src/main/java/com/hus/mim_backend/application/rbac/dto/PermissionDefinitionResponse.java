package com.hus.mim_backend.application.rbac.dto;

/**
 * RBAC permission response DTO.
 */
public class PermissionDefinitionResponse {
    private String name;
    private String description;
    private String resource;
    private String action;
    private boolean delegable;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public boolean isDelegable() {
        return delegable;
    }

    public void setDelegable(boolean delegable) {
        this.delegable = delegable;
    }
}
