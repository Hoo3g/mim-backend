package com.hus.mim_backend.infrastructure.adapter.persistence.auth;

import com.hus.mim_backend.application.port.output.UserRepository;
import com.hus.mim_backend.domain.auth.model.Email;
import com.hus.mim_backend.domain.auth.model.User;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * JPA adapter implementing UserRepository port
 */
@Component
public class UserJpaAdapter implements UserRepository {

    private final UserJpaRepository jpaRepository;
    private final RoleJpaRepository roleJpaRepository;

    public UserJpaAdapter(UserJpaRepository jpaRepository, RoleJpaRepository roleJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.roleJpaRepository = roleJpaRepository;
    }

    @Override
    public Optional<User> findById(UUID id) {
        return jpaRepository.findById(id).map(UserEntity::toDomain);
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        return jpaRepository.findByEmail(email.getValue()).map(UserEntity::toDomain);
    }

    @Override
    public Optional<User> findByStudentCode(String studentCode) {
        return jpaRepository.findByStudentCode(studentCode).map(UserEntity::toDomain);
    }

    @Override
    public User save(User user) {
        Set<RoleEntity> resolvedRoles = resolveRoleEntities(user.getRoles());
        UserEntity entity = UserEntity.fromDomain(user, resolvedRoles);
        UserEntity saved = jpaRepository.save(entity);
        return saved.toDomain();
    }

    @Override
    public boolean existsByEmail(Email email) {
        return jpaRepository.existsByEmail(email.getValue());
    }

    @Override
    public boolean existsByStudentCode(String studentCode) {
        return jpaRepository.existsByStudentCode(studentCode);
    }

    @Override
    public void upsertStudentCode(UUID userId, String studentCode) {
        jpaRepository.upsertStudentCode(userId, studentCode);
    }

    @Override
    public List<User> findByAccountStatus(String status) {
        // TODO: Implement via JpaRepository query
        return List.of();
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    private Set<RoleEntity> resolveRoleEntities(Set<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
            return Set.of();
        }

        Set<String> normalizedNames = roleNames.stream()
                .filter(name -> name != null && !name.isBlank())
                .map(name -> name.trim().toUpperCase())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (normalizedNames.isEmpty()) {
            return Set.of();
        }

        List<RoleEntity> existingRoles = roleJpaRepository.findByNameIn(normalizedNames);
        Map<String, RoleEntity> byName = existingRoles.stream()
                .collect(Collectors.toMap(RoleEntity::getName, Function.identity(), (a, b) -> a));

        Set<RoleEntity> resolved = new LinkedHashSet<>(existingRoles);
        for (String roleName : normalizedNames) {
            if (!byName.containsKey(roleName)) {
                RoleEntity newRole = new RoleEntity();
                newRole.setId(UUID.randomUUID());
                newRole.setName(roleName);
                newRole.setCreatedAt(LocalDateTime.now());
                resolved.add(roleJpaRepository.save(newRole));
            }
        }
        return resolved;
    }
}
