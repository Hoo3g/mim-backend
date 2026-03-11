package com.hus.mim_backend.infrastructure.adapter.persistence.research;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ResearchCategoryJpaRepository extends JpaRepository<ResearchCategoryEntity, UUID> {
    List<ResearchCategoryEntity> findByActiveTrueOrderBySortOrderAscNameAsc();

    @Query("SELECT c FROM ResearchCategoryEntity c ORDER BY c.active DESC, c.sortOrder ASC, c.name ASC")
    List<ResearchCategoryEntity> findAllForAdminOrder();

    @Query(value = """
            SELECT name
            FROM research_categories
            WHERE active = TRUE
              AND LOWER(name) = LOWER(:categoryName)
            LIMIT 1
            """, nativeQuery = true)
    Optional<String> findActiveCategoryName(@Param("categoryName") String categoryName);

    @Query(value = """
            SELECT name
            FROM research_categories
            WHERE id = :categoryId
            LIMIT 1
            """, nativeQuery = true)
    Optional<String> findCategoryNameById(@Param("categoryId") UUID categoryId);

    @Query(value = """
            SELECT EXISTS (
                SELECT 1
                FROM research_categories
                WHERE LOWER(name) = LOWER(:categoryName)
            )
            """, nativeQuery = true)
    boolean existsByNameIgnoreCase(@Param("categoryName") String categoryName);

    @Query(value = """
            SELECT EXISTS (
                SELECT 1
                FROM research_categories
                WHERE id <> :categoryId
                  AND LOWER(name) = LOWER(:categoryName)
            )
            """, nativeQuery = true)
    boolean existsOtherByNameIgnoreCase(@Param("categoryId") UUID categoryId,
            @Param("categoryName") String categoryName);
}
