package com.hus.mim_backend.infrastructure.adapter.persistence.research;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpecializationJpaRepository extends JpaRepository<SpecializationEntity, UUID> {
    List<SpecializationEntity> findByActiveTrueOrderBySortOrderAscNameAsc();

    @Query("SELECT s FROM SpecializationEntity s ORDER BY s.active DESC, s.sortOrder ASC, s.name ASC")
    List<SpecializationEntity> findAllForAdminOrder();

    @Query(value = """
            SELECT name
            FROM specializations
            WHERE active = TRUE
              AND LOWER(name) = LOWER(:specializationName)
            LIMIT 1
            """, nativeQuery = true)
    Optional<String> findActiveSpecializationName(@Param("specializationName") String specializationName);

    @Query(value = """
            SELECT EXISTS (
                SELECT 1
                FROM specializations
                WHERE LOWER(name) = LOWER(:specializationName)
            )
            """, nativeQuery = true)
    boolean existsByNameIgnoreCase(@Param("specializationName") String specializationName);

    @Query(value = """
            SELECT EXISTS (
                SELECT 1
                FROM specializations
                WHERE id <> :specializationId
                  AND LOWER(name) = LOWER(:specializationName)
            )
            """, nativeQuery = true)
    boolean existsOtherByNameIgnoreCase(@Param("specializationId") UUID specializationId,
            @Param("specializationName") String specializationName);

    @Modifying
    @Transactional
    @Query(value = """
            DELETE FROM specializations
            WHERE id = :specializationId
            """, nativeQuery = true)
    int deleteByIdReturningCount(@Param("specializationId") UUID specializationId);
}
