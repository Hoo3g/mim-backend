package com.hus.mim_backend.application.rbac.dto;

import java.util.List;

/**
 * Request DTO for replacing user permission overrides.
 */
public class UpdateUserPermissionOverridesRequest {
    private List<String> grants;
    private List<String> denies;

    public List<String> getGrants() {
        return grants;
    }

    public void setGrants(List<String> grants) {
        this.grants = grants;
    }

    public List<String> getDenies() {
        return denies;
    }

    public void setDenies(List<String> denies) {
        this.denies = denies;
    }
}
