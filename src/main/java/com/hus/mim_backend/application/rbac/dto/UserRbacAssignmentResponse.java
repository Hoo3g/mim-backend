package com.hus.mim_backend.application.rbac.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * User RBAC assignment response DTO.
 */
@Setter
@Getter
public class UserRbacAssignmentResponse {
    private UUID userId;
    private String displayName;
    private String email;
    private String accountStatus;
    private List<String> roles;
    private List<String> effectivePermissions;
    private List<UserPermissionOverrideResponse> overrides;
    private LocalDateTime createdAt;

}
