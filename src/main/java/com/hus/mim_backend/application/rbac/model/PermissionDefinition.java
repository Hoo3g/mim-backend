package com.hus.mim_backend.application.rbac.model;

/**
 * Permission definition model from RBAC catalog.
 */
public class PermissionDefinition {
    private String name;
    private String description;
    private String resource;
    private String action;

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
}
