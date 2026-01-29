package com.hus.mim_backend.application.port.output;

import com.hus.mim_backend.domain.auth.model.Email;
import com.hus.mim_backend.domain.auth.model.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Output port for User persistence operations
 */
public interface UserRepository {
    Optional<User> findById(UUID id);

    Optional<User> findByEmail(Email email);

    User save(User user);

    boolean existsByEmail(Email email);

    List<User> findByAccountStatus(String status);

    void deleteById(UUID id);
}
