package com.hus.mim_backend.application.port.output;

import com.hus.mim_backend.domain.research.model.ResearchPaper;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Output port for ResearchPaper persistence operations
 */
public interface ResearchPaperRepository {
    Optional<ResearchPaper> findById(UUID id);

    List<ResearchPaper> findByCategory(String category);

    List<ResearchPaper> findByResearchArea(String researchArea);

    List<ResearchPaper> findByApprovalStatus(String approvalStatus);

    List<ResearchPaper> findByPublicationYear(Integer year);

    List<ResearchPaper> searchByTitle(String keyword);

    ResearchPaper save(ResearchPaper paper);

    void deleteById(UUID id);

    void incrementViewCount(UUID id);

    void incrementDownloadCount(UUID id);
}
