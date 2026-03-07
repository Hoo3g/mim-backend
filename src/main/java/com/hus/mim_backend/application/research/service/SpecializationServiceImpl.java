package com.hus.mim_backend.application.research.service;

import com.hus.mim_backend.application.port.output.SpecializationRepository;
import com.hus.mim_backend.application.research.dto.CreateResearchCategoryRequest;
import com.hus.mim_backend.application.research.dto.ResearchCategoryResponse;
import com.hus.mim_backend.application.research.dto.UpdateResearchCategoryRequest;
import com.hus.mim_backend.application.research.usecase.ManageSpecializationUseCase;
import com.hus.mim_backend.application.research.usecase.QuerySpecializationUseCase;
import com.hus.mim_backend.domain.shared.DomainException;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Application service for specialization taxonomy.
 */
public class SpecializationServiceImpl implements QuerySpecializationUseCase, ManageSpecializationUseCase {
    private static final int MAX_NAME_LENGTH = 120;

    private final SpecializationRepository repository;

    public SpecializationServiceImpl(SpecializationRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<ResearchCategoryResponse> getActiveSpecializations() {
        return repository.findActiveSpecializations();
    }

    @Override
    public List<ResearchCategoryResponse> getAllSpecializations() {
        return repository.findAllSpecializations();
    }

    @Override
    public ResearchCategoryResponse createSpecialization(CreateResearchCategoryRequest request) {
        if (request == null) {
            throw new DomainException("Request body is required");
        }

        String name = normalizeName(request.getName());
        int sortOrder = normalizeSortOrder(request.getSortOrder());
        boolean active = request.getActive() == null || request.getActive();

        if (repository.existsSpecializationWithSameName(name)) {
            throw new DomainException("Specialization already exists: " + name);
        }

        UUID id = repository.createSpecialization(name, sortOrder, active);
        return repository.findById(id).orElseThrow(() -> new DomainException("Specialization not found"));
    }

    @Override
    public Optional<ResearchCategoryResponse> updateSpecialization(UUID specializationId, UpdateResearchCategoryRequest request) {
        if (specializationId == null) {
            throw new DomainException("specializationId is required");
        }
        if (request == null) {
            throw new DomainException("Request body is required");
        }

        Optional<ResearchCategoryResponse> current = repository.findById(specializationId);
        if (current.isEmpty()) {
            return Optional.empty();
        }

        String name = normalizeName(request.getName());
        int sortOrder = normalizeSortOrder(request.getSortOrder());
        boolean active = request.getActive() == null ? current.get().isActive() : request.getActive();

        if (repository.existsOtherSpecializationWithSameName(specializationId, name)) {
            throw new DomainException("Specialization already exists: " + name);
        }

        int updated = repository.updateSpecialization(specializationId, name, sortOrder, active);
        if (updated == 0) {
            return Optional.empty();
        }

        return repository.findById(specializationId);
    }

    @Override
    public boolean deactivateSpecialization(UUID specializationId) {
        if (specializationId == null) {
            throw new DomainException("specializationId is required");
        }
        return repository.deactivateSpecialization(specializationId) > 0;
    }

    private String normalizeName(String value) {
        if (!StringUtils.hasText(value)) {
            throw new DomainException("Specialization name is required");
        }

        String normalized = value.trim().replaceAll("\\s+", " ");
        if (normalized.length() > MAX_NAME_LENGTH) {
            throw new DomainException("Specialization name exceeds 120 characters");
        }
        return normalized;
    }

    private int normalizeSortOrder(Integer value) {
        if (value == null) {
            return 0;
        }
        if (value < 0) {
            throw new DomainException("sortOrder must be greater than or equal to 0");
        }
        return value;
    }
}
