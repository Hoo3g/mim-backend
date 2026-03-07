package com.hus.mim_backend.application.research.usecase;

import com.hus.mim_backend.application.research.dto.ResearchBookmarkResponse;

import java.util.List;
import java.util.UUID;

public interface ResearchBookmarkUseCase {
    void bookmarkPaper(String email, UUID paperId);

    void unbookmarkPaper(String email, UUID paperId);

    List<ResearchBookmarkResponse> getMyBookmarks(String email);
}
