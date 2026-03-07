package com.hus.mim_backend.application.research.service;

import com.hus.mim_backend.application.port.output.ResearchBookmarkRepository;
import com.hus.mim_backend.application.research.dto.ResearchBookmarkResponse;
import com.hus.mim_backend.application.research.usecase.ResearchBookmarkUseCase;
import com.hus.mim_backend.domain.shared.DomainException;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

public class ResearchBookmarkService implements ResearchBookmarkUseCase {
    private final ResearchBookmarkRepository repository;

    public ResearchBookmarkService(ResearchBookmarkRepository repository) {
        this.repository = repository;
    }

    @Override
    public void bookmarkPaper(String email, UUID paperId) {
        UUID userId = resolveUserId(email);
        if (!repository.existsApprovedPaper(paperId)) {
            throw new DomainException("Research paper not found or not approved");
        }
        repository.saveBookmark(userId, paperId);
    }

    @Override
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
