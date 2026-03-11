package com.hus.mim_backend.domain.role.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Role aggregate - RBAC role
 */
@Getter
@Setter
public class Role {
    private UUID id;
    private String name;
    private String description;
    private Set<Permission> permissions;
    private LocalDateTime createdAt;

    public Role() {
    }

    public static RoleBuilder builder() {
        return new RoleBuilder();
    }

    public boolean hasPermission(String permissionName) {
        if (permissionName == null || permissionName.isBlank()) {
            return false;
        }
        if (permissions == null || permissions.isEmpty()) {
            return false;
        }

        String normalized = permissionName.trim().toUpperCase(Locale.ROOT);
        return permissions.stream()
                .map(Permission::getName)
                .filter(Objects::nonNull)
                .map((name) -> name.trim().toUpperCase(Locale.ROOT))
                .anyMatch(normalized::equals);
    }

    public void addPermission(Permission permission) {
        if (permission == null || permission.getName() == null || permission.getName().isBlank()) {
            return;
        }
        if (permissions == null) {
            permissions = new LinkedHashSet<>();
        }
        if (!hasPermission(permission.getName())) {
            permissions.add(permission);
        }
    }

    public void removePermission(String permissionName) {
        if (permissionName == null || permissionName.isBlank() || permissions == null || permissions.isEmpty()) {
            return;
        }
        String normalized = permissionName.trim().toUpperCase(Locale.ROOT);
        permissions.removeIf((permission) -> permission != null
                && permission.getName() != null
                && permission.getName().trim().toUpperCase(Locale.ROOT).equals(normalized));
    }

    public static class RoleBuilder {
        private final Role role = new Role();

        public RoleBuilder id(UUID id) {
            role.id = id;
            return this;
        }

        public RoleBuilder name(String name) {
            role.name = name;
            return this;
        }

        public RoleBuilder description(String description) {
            role.description = description;
            return this;
        }

        public RoleBuilder permissions(Set<Permission> permissions) {
            role.permissions = permissions;
            return this;
        }

        public RoleBuilder createdAt(LocalDateTime createdAt) {
            role.createdAt = createdAt;
            return this;
        }

        public Role build() {
            return role;
        }
    }
}
