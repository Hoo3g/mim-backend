package com.hus.mim_backend.application.rbac.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * User RBAC assignment projection.
 */
@Setter
@Getter
public class UserRbacAssignment {
    private UUID userId;
    private String displayName;
    private String email;
    private String accountStatus;
    private List<String> roles;
    private LocalDateTime createdAt;

}
