package com.hus.mim_backend.application.research.service;

import com.hus.mim_backend.application.port.output.ResearchBookmarkRepository;
import com.hus.mim_backend.application.research.dto.ResearchBookmarkResponse;
import com.hus.mim_backend.application.research.usecase.ResearchBookmarkUseCase;
import com.hus.mim_backend.domain.shared.DomainException;
import com.hus.mim_backend.infrastructure.config.CacheNames;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

public class ResearchBookmarkService implements ResearchBookmarkUseCase {
    private final ResearchBookmarkRepository repository;

    public ResearchBookmarkService(ResearchBookmarkRepository repository) {
        this.repository = repository;
    }

    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.PUBLIC_RESEARCH_PAPERS, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.PUBLIC_RESEARCH_PAPER_DETAILS,
                    key = "T(com.hus.mim_backend.infrastructure.config.CacheKeys).idKey(#paperId)")
    })
    public void bookmarkPaper(String email, UUID paperId) {
        UUID userId = resolveUserId(email);
        if (!repository.existsApprovedPaper(paperId)) {
            throw new DomainException("Research paper not found or not approved");
        }
        repository.saveBookmark(userId, paperId);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.PUBLIC_RESEARCH_PAPERS, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.PUBLIC_RESEARCH_PAPER_DETAILS,
                    key = "T(com.hus.mim_backend.infrastructure.config.CacheKeys).idKey(#paperId)")
    })
    public void unbookmarkPaper(String email, UUID paperId) {
        UUID userId = resolveUserId(email);
        repository.deleteBookmark(userId, paperId);
    }

    @Override
    public List<ResearchBookmarkResponse> getMyBookmarks(String email) {
        UUID userId = resolveUserId(email);
        return repository.findBookmarksByUserId(userId);
    }

    private UUID resolveUserId(String email) {
        if (!StringUtils.hasText(email)) {
            throw new DomainException("Authentication required");
        }
        return repository.findUserIdByEmail(email.trim())
                .orElseThrow(() -> new DomainException("Authenticated user is not found"));
    }
}
