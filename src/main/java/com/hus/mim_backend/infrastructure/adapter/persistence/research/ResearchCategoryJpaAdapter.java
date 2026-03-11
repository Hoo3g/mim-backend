package com.hus.mim_backend.infrastructure.adapter.persistence.research;

import com.hus.mim_backend.application.port.output.ResearchCategoryRepository;
import com.hus.mim_backend.application.research.dto.ResearchCategoryResponse;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class ResearchCategoryJpaAdapter implements ResearchCategoryRepository {
    private final ResearchCategoryJpaRepository repository;

    public ResearchCategoryJpaAdapter(ResearchCategoryJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<ResearchCategoryResponse> findActiveCategories() {
        return repository.findByActiveTrueOrderBySortOrderAscNameAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<ResearchCategoryResponse> findAllCategories() {
        return repository.findAllForAdminOrder().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public Optional<ResearchCategoryResponse> findById(UUID categoryId) {
        return repository.findById(categoryId).map(this::toResponse);
    }

    @Override
    public Optional<String> findActiveCategoryName(String categoryName) {
        return repository.findActiveCategoryName(categoryName);
    }

    @Override
    public Optional<String> findCategoryNameById(UUID categoryId) {
        return repository.findCategoryNameById(categoryId);
    }

    @Override
    public boolean existsCategoryWithSameName(String categoryName) {
        return repository.existsByNameIgnoreCase(categoryName);
    }

    @Override
    public boolean existsOtherCategoryWithSameName(UUID categoryId, String categoryName) {
        return repository.existsOtherByNameIgnoreCase(categoryId, categoryName);
    }

    @Override
    @Transactional
    public UUID createCategory(String categoryName, int sortOrder, boolean active) {
        ResearchCategoryEntity entity = new ResearchCategoryEntity();
        entity.setId(UUID.randomUUID());
        entity.setName(categoryName);
        entity.setSortOrder(sortOrder);
        entity.setActive(active);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        return repository.save(entity).getId();
    }

    @Override
    @Transactional
    public int updateCategory(UUID categoryId, String categoryName, int sortOrder, boolean active) {
        Optional<ResearchCategoryEntity> existing = repository.findById(categoryId);
        if (existing.isEmpty()) {
            return 0;
        }

        ResearchCategoryEntity entity = existing.get();
        entity.setName(categoryName);
        entity.setSortOrder(sortOrder);
        entity.setActive(active);
        entity.setUpdatedAt(LocalDateTime.now());
        repository.save(entity);
        return 1;
    }

    @Override
    @Transactional
    public int deactivateCategory(UUID categoryId) {
        Optional<ResearchCategoryEntity> existing = repository.findById(categoryId);
        if (existing.isEmpty()) {
            return 0;
        }
        ResearchCategoryEntity entity = existing.get();
        entity.setActive(false);
        entity.setUpdatedAt(LocalDateTime.now());
        repository.save(entity);
        return 1;
    }

    private ResearchCategoryResponse toResponse(ResearchCategoryEntity entity) {
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
