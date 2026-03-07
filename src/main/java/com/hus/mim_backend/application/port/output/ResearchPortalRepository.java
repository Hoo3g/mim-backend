package com.hus.mim_backend.application.port.output;

import com.hus.mim_backend.application.research.dto.PaperResponse;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Output port for research portal read/write operations.
 */
public interface ResearchPortalRepository {
    List<PaperResponse> findAllApprovedPapers();

    List<PaperResponse> findMyPapers(UUID userId);

    Optional<PaperResponse> findApprovedPaperById(UUID paperId);

    Optional<PaperResponse> findPaperById(UUID paperId);

    List<PaperResponse.PaperAuthorResponse> findAuthorsByPaperId(UUID paperId);

    Optional<UUID> findUserIdByEmail(String email);

    boolean hasRole(UUID userId, String roleName);

    void upsertStudentProfile(UUID userId);

    void upsertLecturerProfile(UUID userId);

    Optional<String> findActiveResearchCategoryName(String researchAreaName);

    UUID createPaperWithMainAuthor(UUID userId,
                                   boolean lecturerAuthor,
                                   String title,
                                   String abstractText,
                                   String pdfUrl,
                                   int publicationYear,
                                   String journalConference,
                                   String researchArea,
                                   String category);

    boolean isOwner(UUID paperId, UUID userId);

    int updatePaper(UUID paperId, String title, String abstractText, String pdfUrl, String researchArea);
}
