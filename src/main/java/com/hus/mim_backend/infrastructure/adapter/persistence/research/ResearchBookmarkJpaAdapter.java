package com.hus.mim_backend.infrastructure.adapter.persistence.research;

import com.hus.mim_backend.application.port.output.ResearchBookmarkRepository;
import com.hus.mim_backend.application.research.dto.ResearchBookmarkResponse;
import com.hus.mim_backend.infrastructure.adapter.persistence.auth.UserJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class ResearchBookmarkJpaAdapter implements ResearchBookmarkRepository {
    private final SavedResearchPaperJpaRepository repository;
    private final UserJpaRepository userJpaRepository;

    public ResearchBookmarkJpaAdapter(SavedResearchPaperJpaRepository repository, UserJpaRepository userJpaRepository) {
        this.repository = repository;
        this.userJpaRepository = userJpaRepository;
    }

    @Override
    public Optional<UUID> findUserIdByEmail(String email) {
        return userJpaRepository.findByEmail(email).map(user -> user.getId());
    }

    @Override
    public boolean existsApprovedPaper(UUID paperId) {
        return repository.existsApprovedPaper(paperId);
    }

    @Override
    public void saveBookmark(UUID userId, UUID paperId) {
        repository.insertIgnoreConflict(userId, paperId);
    }

    @Override
    public void deleteBookmark(UUID userId, UUID paperId) {
        repository.deleteByUserIdAndPaperId(userId, paperId);
    }

    @Override
    public List<ResearchBookmarkResponse> findBookmarksByUserId(UUID userId) {
        return repository.findBookmarksByUserId(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    private ResearchBookmarkResponse toResponse(ResearchBookmarkProjection projection) {
        ResearchBookmarkResponse response = new ResearchBookmarkResponse();
        response.setPaperId(projection.getPaperId());
        response.setTitle(projection.getTitle());
        response.setResearchArea(projection.getResearchArea());
        response.setCategory(projection.getCategory());
        response.setPublicationYear(projection.getPublicationYear());
        response.setSavedAt(projection.getSavedAt());
        return response;
    }
}
