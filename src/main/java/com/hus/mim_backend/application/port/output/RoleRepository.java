package com.hus.mim_backend.application.port.output;

import com.hus.mim_backend.domain.role.model.Role;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Output port for Role persistence operations
 */
public interface RoleRepository {
    Optional<Role> findById(UUID id);

    Optional<Role> findByName(String name);

    List<Role> findAll();

    Role save(Role role);

    void deleteById(UUID id);
}
