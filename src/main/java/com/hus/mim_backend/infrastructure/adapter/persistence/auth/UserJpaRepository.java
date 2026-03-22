package com.hus.mim_backend.infrastructure.adapter.persistence.auth;

import com.hus.mim_backend.domain.auth.model.AccountStatus;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository for UserEntity
 */
public interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {
    Optional<UserEntity> findByEmail(String email);

    @Query("SELECT u.id FROM UserEntity u WHERE UPPER(u.email) = UPPER(:email)")
    Optional<UUID> findIdByEmail(@Param("email") String email);

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

    @Query(value = "SELECT EXISTS(SELECT 1 FROM students WHERE id = :userId)", nativeQuery = true)
    boolean hasStudentRegistration(@Param("userId") UUID userId);

    @Query(value = "SELECT EXISTS(SELECT 1 FROM lecturers WHERE id = :userId)", nativeQuery = true)
    boolean hasLecturerRegistration(@Param("userId") UUID userId);

    @Query(value = "SELECT EXISTS(SELECT 1 FROM companies WHERE id = :userId)", nativeQuery = true)
    boolean hasCompanyRegistration(@Param("userId") UUID userId);

    List<UserEntity> findByStatus(AccountStatus status);

    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO students (id, first_name, last_name, major, updated_at)
            VALUES (:userId, :firstName, :lastName, :major, CURRENT_TIMESTAMP)
            ON CONFLICT (id) DO UPDATE
            SET first_name = EXCLUDED.first_name,
                last_name = EXCLUDED.last_name,
                major = COALESCE(EXCLUDED.major, students.major),
                updated_at = CURRENT_TIMESTAMP
            """, nativeQuery = true)
    void upsertStudentRegistration(@Param("userId") UUID userId,
            @Param("firstName") String firstName,
            @Param("lastName") String lastName,
            @Param("major") String major);

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

    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO lecturers (id, first_name, last_name, title, updated_at)
            VALUES (:userId, :firstName, :lastName, :title, CURRENT_TIMESTAMP)
            ON CONFLICT (id) DO UPDATE
            SET first_name = EXCLUDED.first_name,
                last_name = EXCLUDED.last_name,
                title = EXCLUDED.title,
                updated_at = CURRENT_TIMESTAMP
            """, nativeQuery = true)
    void upsertLecturerRegistration(@Param("userId") UUID userId,
            @Param("firstName") String firstName,
            @Param("lastName") String lastName,
            @Param("title") String title);

    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO companies (id, name, website, updated_at)
            VALUES (:userId, :companyName, :website, CURRENT_TIMESTAMP)
            ON CONFLICT (id) DO UPDATE
            SET name = EXCLUDED.name,
                website = COALESCE(EXCLUDED.website, companies.website),
                updated_at = CURRENT_TIMESTAMP
            """, nativeQuery = true)
    void upsertCompanyRegistration(@Param("userId") UUID userId,
            @Param("companyName") String companyName,
            @Param("website") String website);
}
