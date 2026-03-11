package com.hus.mim_backend.application.rbac.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Request DTO for replacing user permission overrides.
 */
@Setter
@Getter
public class UpdateUserPermissionOverridesRequest {
    private List<String> grants;
    private List<String> denies;

}
