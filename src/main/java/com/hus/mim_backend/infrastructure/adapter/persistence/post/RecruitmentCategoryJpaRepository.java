package com.hus.mim_backend.infrastructure.adapter.persistence.post;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface RecruitmentCategoryJpaRepository extends JpaRepository<RecruitmentCategoryEntity, UUID> {
    List<RecruitmentCategoryEntity> findByActiveTrueOrderBySortOrderAscNameAsc();

    @Query("""
            SELECT rc
            FROM RecruitmentCategoryEntity rc
            ORDER BY CASE WHEN rc.active = true THEN 0 ELSE 1 END, rc.sortOrder ASC, rc.name ASC
            """)
    List<RecruitmentCategoryEntity> findAllForAdminOrder();

    @Query("""
            SELECT COUNT(rc) > 0
            FROM RecruitmentCategoryEntity rc
            WHERE LOWER(rc.name) = LOWER(:recruitmentCategoryName)
            """)
    boolean existsByNameIgnoreCase(@Param("recruitmentCategoryName") String recruitmentCategoryName);

    @Query("""
            SELECT COUNT(rc) > 0
            FROM RecruitmentCategoryEntity rc
            WHERE rc.id <> :recruitmentCategoryId
              AND LOWER(rc.name) = LOWER(:recruitmentCategoryName)
            """)
    boolean existsOtherByNameIgnoreCase(@Param("recruitmentCategoryId") UUID recruitmentCategoryId,
                                        @Param("recruitmentCategoryName") String recruitmentCategoryName);

    @Query("""
            SELECT rc.name
            FROM RecruitmentCategoryEntity rc
            WHERE rc.active = true
              AND LOWER(rc.name) IN :normalizedNames
            """)
    List<String> findActiveNamesByLowerNameIn(@Param("normalizedNames") Collection<String> normalizedNames);
}
