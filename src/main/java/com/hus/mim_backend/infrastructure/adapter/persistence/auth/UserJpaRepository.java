package com.hus.mim_backend.infrastructure.adapter.persistence.auth;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository for UserEntity
 */
public interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {
    Optional<UserEntity> findByEmail(String email);

    @Query(value = """
            SELECT u.*
            FROM users u
            JOIN students s ON s.id = u.id
            WHERE upper(s.student_code) = upper(:studentCode)
            """, nativeQuery = true)
    Optional<UserEntity> findByStudentCode(@Param("studentCode") String studentCode);

    boolean existsByEmail(String email);

    @Query(value = """
            SELECT EXISTS(
                SELECT 1
                FROM students s
                WHERE upper(s.student_code) = upper(:studentCode)
            )
            """, nativeQuery = true)
    boolean existsByStudentCode(@Param("studentCode") String studentCode);

    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO students (id, student_code, updated_at)
            VALUES (:userId, :studentCode, CURRENT_TIMESTAMP)
            ON CONFLICT (id) DO UPDATE
            SET student_code = EXCLUDED.student_code,
                updated_at = CURRENT_TIMESTAMP
            """, nativeQuery = true)
    void upsertStudentCode(@Param("userId") UUID userId, @Param("studentCode") String studentCode);
}
