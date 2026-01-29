package com.hus.mim_backend.application.research.usecase;

import com.hus.mim_backend.application.research.dto.CreatePaperRequest;
import com.hus.mim_backend.application.research.dto.PaperResponse;
import java.util.List;
import java.util.UUID;

/**
 * Input port for managing research papers
 */
public interface ManageResearchPaperUseCase {
    PaperResponse uploadPaper(CreatePaperRequest request);

    PaperResponse getPaper(UUID paperId);

    List<PaperResponse> searchPapers(String keyword);

    void trackView(UUID paperId);

    void trackDownload(UUID paperId);
}
