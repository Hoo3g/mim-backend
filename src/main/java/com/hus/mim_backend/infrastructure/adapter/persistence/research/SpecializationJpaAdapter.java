package com.hus.mim_backend.infrastructure.adapter.persistence.research;

import com.hus.mim_backend.application.port.output.SpecializationRepository;
import com.hus.mim_backend.application.research.dto.ResearchCategoryResponse;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class SpecializationJpaAdapter implements SpecializationRepository {
    private final SpecializationJpaRepository repository;

    public SpecializationJpaAdapter(SpecializationJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<ResearchCategoryResponse> findActiveSpecializations() {
        return repository.findByActiveTrueOrderBySortOrderAscNameAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<ResearchCategoryResponse> findAllSpecializations() {
        return repository.findAllForAdminOrder().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public Optional<ResearchCategoryResponse> findById(UUID specializationId) {
        return repository.findById(specializationId).map(this::toResponse);
    }

    @Override
    public Optional<String> findActiveSpecializationName(String specializationName) {
        return repository.findActiveSpecializationName(specializationName);
    }

    @Override
    public boolean existsSpecializationWithSameName(String specializationName) {
        return repository.existsByNameIgnoreCase(specializationName);
    }

    @Override
    public boolean existsOtherSpecializationWithSameName(UUID specializationId, String specializationName) {
        return repository.existsOtherByNameIgnoreCase(specializationId, specializationName);
    }

    @Override
    @Transactional
    public UUID createSpecialization(String specializationName, int sortOrder, boolean active) {
        SpecializationEntity entity = new SpecializationEntity();
        entity.setId(UUID.randomUUID());
        entity.setName(specializationName);
        entity.setSortOrder(sortOrder);
        entity.setActive(active);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        return repository.save(entity).getId();
    }

    @Override
    @Transactional
    public int updateSpecialization(UUID specializationId, String specializationName, int sortOrder, boolean active) {
        Optional<SpecializationEntity> existing = repository.findById(specializationId);
        if (existing.isEmpty()) {
            return 0;
        }
        SpecializationEntity entity = existing.get();
        entity.setName(specializationName);
        entity.setSortOrder(sortOrder);
        entity.setActive(active);
        entity.setUpdatedAt(LocalDateTime.now());
        repository.save(entity);
        return 1;
    }

    @Override
    @Transactional
    public int deleteSpecialization(UUID specializationId) {
        return repository.deleteByIdReturningCount(specializationId);
    }

    private ResearchCategoryResponse toResponse(SpecializationEntity entity) {
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
