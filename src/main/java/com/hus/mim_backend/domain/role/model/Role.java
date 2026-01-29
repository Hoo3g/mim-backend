package com.hus.mim_backend.domain.role.model;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * Role aggregate - RBAC role
 */
public class Role {
    private UUID id;
    private String name;
    private String description;
    private Set<Permission> permissions;
    private LocalDateTime createdAt;

    // TODO: Implement getters/setters
    // TODO: Implement builder pattern
    // TODO: Implement domain logic methods
}
