package com.hus.mim_backend.application.port.output;

import com.hus.mim_backend.application.research.dto.ResearchBookmarkResponse;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ResearchBookmarkRepository {
    Optional<UUID> findUserIdByEmail(String email);

    boolean existsApprovedPaper(UUID paperId);

    void saveBookmark(UUID userId, UUID paperId);

    void deleteBookmark(UUID userId, UUID paperId);

    List<ResearchBookmarkResponse> findBookmarksByUserId(UUID userId);
}
