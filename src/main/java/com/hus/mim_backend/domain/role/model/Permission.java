package com.hus.mim_backend.domain.role.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Permission aggregate - RBAC permission
 */
public class Permission {
    private UUID id;
    private String name;
    private String description;
    private LocalDateTime createdAt;

    // TODO: Implement getters/setters
    // TODO: Implement builder pattern
}
