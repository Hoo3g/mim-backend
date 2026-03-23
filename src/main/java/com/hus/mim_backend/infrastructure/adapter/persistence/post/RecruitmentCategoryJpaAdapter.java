package com.hus.mim_backend.infrastructure.adapter.persistence.post;

import com.hus.mim_backend.application.port.output.RecruitmentCategoryRepository;
import com.hus.mim_backend.application.research.dto.ResearchCategoryResponse;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class RecruitmentCategoryJpaAdapter implements RecruitmentCategoryRepository {
    private final RecruitmentCategoryJpaRepository repository;

    public RecruitmentCategoryJpaAdapter(RecruitmentCategoryJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<ResearchCategoryResponse> findActiveRecruitmentCategories() {
        return repository.findByActiveTrueOrderBySortOrderAscNameAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<ResearchCategoryResponse> findAllRecruitmentCategories() {
        return repository.findAllForAdminOrder().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public Optional<ResearchCategoryResponse> findById(UUID recruitmentCategoryId) {
        return repository.findById(recruitmentCategoryId).map(this::toResponse);
    }

    @Override
    public List<String> findActiveRecruitmentCategoryNames(List<String> recruitmentCategoryNames) {
        if (recruitmentCategoryNames == null || recruitmentCategoryNames.isEmpty()) {
            return List.of();
        }

        LinkedHashSet<String> normalizedNames = recruitmentCategoryNames.stream()
                .map((name) -> name == null ? "" : name.trim().toLowerCase(Locale.ROOT))
                .filter((name) -> !name.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (normalizedNames.isEmpty()) {
            return List.of();
        }

        return repository.findActiveNamesByLowerNameIn(normalizedNames);
    }

    @Override
    public boolean existsRecruitmentCategoryWithSameName(String recruitmentCategoryName) {
        return repository.existsByNameIgnoreCase(recruitmentCategoryName);
    }

    @Override
    public boolean existsOtherRecruitmentCategoryWithSameName(UUID recruitmentCategoryId, String recruitmentCategoryName) {
        return repository.existsOtherByNameIgnoreCase(recruitmentCategoryId, recruitmentCategoryName);
    }

    @Override
    @Transactional
    public UUID createRecruitmentCategory(String recruitmentCategoryName, int sortOrder, boolean active) {
        RecruitmentCategoryEntity entity = new RecruitmentCategoryEntity();
        entity.setId(UUID.randomUUID());
        entity.setName(recruitmentCategoryName);
        entity.setSortOrder(sortOrder);
        entity.setActive(active);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        return repository.save(entity).getId();
    }

    @Override
    @Transactional
    public int updateRecruitmentCategory(UUID recruitmentCategoryId, String recruitmentCategoryName, int sortOrder, boolean active) {
        Optional<RecruitmentCategoryEntity> existing = repository.findById(recruitmentCategoryId);
        if (existing.isEmpty()) {
            return 0;
        }

        RecruitmentCategoryEntity entity = existing.get();
        entity.setName(recruitmentCategoryName);
        entity.setSortOrder(sortOrder);
        entity.setActive(active);
        entity.setUpdatedAt(LocalDateTime.now());
        repository.save(entity);
        return 1;
    }

    @Override
    @Transactional
    public int deleteRecruitmentCategory(UUID recruitmentCategoryId) {
        Optional<RecruitmentCategoryEntity> existing = repository.findById(recruitmentCategoryId);
        if (existing.isEmpty()) {
            return 0;
        }
        repository.delete(existing.get());
        return 1;
    }

    private ResearchCategoryResponse toResponse(RecruitmentCategoryEntity entity) {
        ResearchCategoryResponse response = new ResearchCategoryResponse();
        response.setId(entity.getId());
        response.setName(entity.getName());
        response.setSortOrder(entity.getSortOrder());
        response.setActive(entity.isActive());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }
}
