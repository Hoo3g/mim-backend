package com.hus.mim_backend.application.research.service;

import com.hus.mim_backend.application.port.output.ResearchCategoryRepository;
import com.hus.mim_backend.application.research.dto.CreateResearchCategoryRequest;
import com.hus.mim_backend.application.research.dto.ResearchCategoryResponse;
import com.hus.mim_backend.application.research.dto.UpdateResearchCategoryRequest;
import com.hus.mim_backend.application.research.usecase.ManageResearchCategoryUseCase;
import com.hus.mim_backend.application.research.usecase.QueryResearchCategoryUseCase;
import com.hus.mim_backend.domain.shared.DomainException;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Application service for research category taxonomy.
 */
public class ResearchCategoryServiceImpl implements QueryResearchCategoryUseCase, ManageResearchCategoryUseCase {
    private static final int MAX_CATEGORY_NAME_LENGTH = 120;

    private final ResearchCategoryRepository repository;

    public ResearchCategoryServiceImpl(ResearchCategoryRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<ResearchCategoryResponse> getActiveCategories() {
        return repository.findActiveCategories();
    }

    @Override
    public List<ResearchCategoryResponse> getAllCategories() {
        return repository.findAllCategories();
    }

    @Override
    public ResearchCategoryResponse createCategory(CreateResearchCategoryRequest request) {
        if (request == null) {
            throw new DomainException("Request body is required");
        }

        String name = normalizeCategoryName(request.getName());
        int sortOrder = normalizeSortOrder(request.getSortOrder());
        boolean active = request.getActive() == null || request.getActive();

        if (repository.existsCategoryWithSameName(name)) {
            throw new DomainException("Category already exists: " + name);
        }

        UUID categoryId = repository.createCategory(name, sortOrder, active);
        return repository.findById(categoryId)
                .orElseThrow(() -> new DomainException("Category not found"));
    }

    @Override
    public Optional<ResearchCategoryResponse> updateCategory(UUID categoryId, UpdateResearchCategoryRequest request) {
        if (categoryId == null) {
            throw new DomainException("categoryId is required");
        }
        if (request == null) {
            throw new DomainException("Request body is required");
        }

        Optional<ResearchCategoryResponse> current = repository.findById(categoryId);
        if (current.isEmpty()) {
            return Optional.empty();
        }

        String name = normalizeCategoryName(request.getName());
        int sortOrder = normalizeSortOrder(request.getSortOrder());
        boolean active = request.getActive() == null ? current.get().isActive() : request.getActive();

        if (repository.existsOtherCategoryWithSameName(categoryId, name)) {
            throw new DomainException("Category already exists: " + name);
        }

        int updated = repository.updateCategory(categoryId, name, sortOrder, active);
        if (updated == 0) {
            return Optional.empty();
        }

        return repository.findById(categoryId);
    }

    @Override
    public boolean deactivateCategory(UUID categoryId) {
        if (categoryId == null) {
            throw new DomainException("categoryId is required");
        }
        return repository.deactivateCategory(categoryId) > 0;
    }

    private String normalizeCategoryName(String value) {
        if (!StringUtils.hasText(value)) {
            throw new DomainException("Category name is required");
        }

        String normalized = value.trim().replaceAll("\\s+", " ");
        if (normalized.length() > MAX_CATEGORY_NAME_LENGTH) {
            throw new DomainException("Category name exceeds 120 characters");
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
