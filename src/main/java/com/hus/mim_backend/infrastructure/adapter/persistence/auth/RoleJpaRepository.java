package com.hus.mim_backend.infrastructure.adapter.persistence.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data repository for RoleEntity.
 */
public interface RoleJpaRepository extends JpaRepository<RoleEntity, UUID> {
    Optional<RoleEntity> findByName(String name);

    List<RoleEntity> findByNameIn(Collection<String> names);
}
