package com.hus.mim_backend.infrastructure.adapter.persistence.news;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface NewsJpaRepository extends JpaRepository<NewsEntity, UUID> {
    List<NewsEntity> findByStatusOrderByPinnedDescCreatedAtDesc(String status);

    @Query("SELECT n FROM NewsEntity n ORDER BY n.pinned DESC, n.createdAt DESC")
    List<NewsEntity> findAllForAdminOrder();

    @Modifying
    @Transactional
    @Query("DELETE FROM NewsEntity n WHERE n.id = :id")
    int deleteByIdReturningCount(@Param("id") UUID id);
}
