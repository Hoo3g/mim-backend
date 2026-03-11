package com.hus.mim_backend.infrastructure.adapter.persistence.content;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ResearchHeroSettingsJpaRepository extends JpaRepository<ResearchHeroSettingsEntity, UUID> {
    Optional<ResearchHeroSettingsEntity> findByPageKey(String pageKey);
}
