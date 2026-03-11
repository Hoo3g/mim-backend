package com.hus.mim_backend.domain.role.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Permission aggregate - RBAC permission
 */
@Getter
@Setter
public class Permission {
    private UUID id;
    private String name;
    private String description;
    private LocalDateTime createdAt;

    public Permission() {
    }

    public static PermissionBuilder builder() {
        return new PermissionBuilder();
    }

    public static class PermissionBuilder {
        private final Permission permission = new Permission();

        public PermissionBuilder id(UUID id) {
            permission.id = id;
            return this;
        }

        public PermissionBuilder name(String name) {
            permission.name = name;
            return this;
        }

        public PermissionBuilder description(String description) {
            permission.description = description;
            return this;
        }

        public PermissionBuilder createdAt(LocalDateTime createdAt) {
            permission.createdAt = createdAt;
            return this;
        }

        public Permission build() {
            return permission;
        }
    }
}
