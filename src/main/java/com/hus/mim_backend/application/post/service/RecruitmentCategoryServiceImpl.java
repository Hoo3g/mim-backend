package com.hus.mim_backend.application.post.service;

import com.hus.mim_backend.application.port.output.RecruitmentCategoryRepository;
import com.hus.mim_backend.application.post.usecase.ManageRecruitmentCategoryUseCase;
import com.hus.mim_backend.application.post.usecase.QueryRecruitmentCategoryUseCase;
import com.hus.mim_backend.application.research.dto.CreateResearchCategoryRequest;
import com.hus.mim_backend.application.research.dto.ResearchCategoryResponse;
import com.hus.mim_backend.application.research.dto.UpdateResearchCategoryRequest;
import com.hus.mim_backend.domain.shared.DomainException;
import com.hus.mim_backend.shared.constants.CacheNames;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Application service for recruitment category taxonomy.
 */
public class RecruitmentCategoryServiceImpl implements QueryRecruitmentCategoryUseCase, ManageRecruitmentCategoryUseCase {
    private static final int MAX_NAME_LENGTH = 120;

    private final RecruitmentCategoryRepository repository;

    public RecruitmentCategoryServiceImpl(RecruitmentCategoryRepository repository) {
        this.repository = repository;
    }

    @Override
    @Cacheable(cacheNames = CacheNames.PUBLIC_RECRUITMENT_CATEGORIES,
            key = "T(com.hus.mim_backend.shared.constants.CacheKeys).singleton()",
            sync = true)
    public List<ResearchCategoryResponse> getActiveRecruitmentCategories() {
        return new ArrayList<>(repository.findActiveRecruitmentCategories());
    }

    @Override
    @Cacheable(cacheNames = CacheNames.RECRUITMENT_CATEGORIES_ALL,
            key = "T(com.hus.mim_backend.shared.constants.CacheKeys).singleton()",
            sync = true)
    public List<ResearchCategoryResponse> getAllRecruitmentCategories() {
        return new ArrayList<>(repository.findAllRecruitmentCategories());
    }

    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.PUBLIC_RECRUITMENT_CATEGORIES, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.RECRUITMENT_CATEGORIES_ALL, allEntries = true)
    })
    @Transactional
    public ResearchCategoryResponse createRecruitmentCategory(CreateResearchCategoryRequest request) {
        if (request == null) {
            throw new DomainException("Request body is required");
        }

        String name = normalizeName(request.getName());
        int sortOrder = normalizeSortOrder(request.getSortOrder());
        boolean active = request.getActive() == null || request.getActive();

        if (repository.existsRecruitmentCategoryWithSameName(name)) {
            throw new DomainException("Recruitment category already exists: " + name);
        }

        UUID id = repository.createRecruitmentCategory(name, sortOrder, active);
        return repository.findById(id).orElseThrow(() -> new DomainException("Recruitment category not found"));
    }

    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.PUBLIC_RECRUITMENT_CATEGORIES, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.RECRUITMENT_CATEGORIES_ALL, allEntries = true)
    })
    @Transactional
    public Optional<ResearchCategoryResponse> updateRecruitmentCategory(UUID recruitmentCategoryId,
                                                                        UpdateResearchCategoryRequest request) {
        if (recruitmentCategoryId == null) {
            throw new DomainException("recruitmentCategoryId is required");
        }
        if (request == null) {
            throw new DomainException("Request body is required");
        }

        Optional<ResearchCategoryResponse> current = repository.findById(recruitmentCategoryId);
        if (current.isEmpty()) {
            return Optional.empty();
        }

        String name = normalizeName(request.getName());
        int sortOrder = normalizeSortOrder(request.getSortOrder());
        boolean active = request.getActive() == null ? current.get().isActive() : request.getActive();

        if (repository.existsOtherRecruitmentCategoryWithSameName(recruitmentCategoryId, name)) {
            throw new DomainException("Recruitment category already exists: " + name);
        }

        int updated = repository.updateRecruitmentCategory(recruitmentCategoryId, name, sortOrder, active);
        if (updated == 0) {
            return Optional.empty();
        }

        return repository.findById(recruitmentCategoryId);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.PUBLIC_RECRUITMENT_CATEGORIES, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.RECRUITMENT_CATEGORIES_ALL, allEntries = true)
    })
    @Transactional
    public boolean deleteRecruitmentCategory(UUID recruitmentCategoryId) {
        if (recruitmentCategoryId == null) {
            throw new DomainException("recruitmentCategoryId is required");
        }
        return repository.deleteRecruitmentCategory(recruitmentCategoryId) > 0;
    }

    private String normalizeName(String value) {
        if (!StringUtils.hasText(value)) {
            throw new DomainException("Recruitment category name is required");
        }

        String normalized = value.trim().replaceAll("\\s+", " ");
        if (normalized.length() > MAX_NAME_LENGTH) {
            throw new DomainException("Recruitment category name exceeds 120 characters");
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
