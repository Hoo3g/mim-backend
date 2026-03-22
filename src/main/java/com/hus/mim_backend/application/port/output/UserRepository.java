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

    /**
     * Lightweight lookup: returns only the user ID for a given email string.
     * Avoids loading the full User entity when only the ID is needed.
     */
    Optional<UUID> findIdByEmail(String email);

    Optional<User> findByStudentCode(String studentCode);

    User save(User user);

    boolean existsByEmail(Email email);

    boolean existsByStudentCode(String studentCode);

    boolean hasStudentRegistration(UUID userId);

    boolean hasLecturerRegistration(UUID userId);

    boolean hasCompanyRegistration(UUID userId);

    void upsertStudentRegistration(UUID userId, String firstName, String lastName, String major);

    void upsertStudentCode(UUID userId, String studentCode);

    void upsertLecturerRegistration(UUID userId, String firstName, String lastName, String title);

    void upsertCompanyRegistration(UUID userId, String companyName, String website);

    List<User> findByAccountStatus(String status);

    void deleteById(UUID id);
}
