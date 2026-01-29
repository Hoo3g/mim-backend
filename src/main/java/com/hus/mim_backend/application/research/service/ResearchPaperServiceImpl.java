package com.hus.mim_backend.application.research.service;

import com.hus.mim_backend.application.research.dto.*;
import com.hus.mim_backend.application.research.usecase.*;
import com.hus.mim_backend.application.port.output.*;
import java.util.List;
import java.util.UUID;

/**
 * Service orchestrating Research Paper use cases
 */
public class ResearchPaperServiceImpl implements ManageResearchPaperUseCase {

    private final ResearchPaperRepository paperRepository;

    public ResearchPaperServiceImpl(ResearchPaperRepository paperRepository) {
        this.paperRepository = paperRepository;
    }

    @Override
    public PaperResponse uploadPaper(CreatePaperRequest request) {
        // TODO: Paper upload and author linking
        return null;
    }

    @Override
    public PaperResponse getPaper(UUID paperId) {
        // TODO: Paper details
        return null;
    }

    @Override
    public List<PaperResponse> searchPapers(String keyword) {
        // TODO: Search papers
        return List.of();
    }

    @Override
    public void trackView(UUID paperId) {
        // TODO: Increment metrics
    }

    @Override
    public void trackDownload(UUID paperId) {
        // TODO: Increment metrics
    }
}
