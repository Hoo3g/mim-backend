package com.hus.mim_backend.application.port.output;

import com.hus.mim_backend.domain.role.model.Permission;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Output port for Permission persistence operations
 */
public interface PermissionRepository {
    Optional<Permission> findById(UUID id);

    Optional<Permission> findByName(String name);

    List<Permission> findAll();

    Permission save(Permission permission);

    void deleteById(UUID id);
}
