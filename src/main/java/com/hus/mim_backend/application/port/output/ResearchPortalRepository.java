package com.hus.mim_backend.application.port.output;

import com.hus.mim_backend.application.research.dto.PaperResponse;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Output port for research portal read/write operations.
 */
public interface ResearchPortalRepository {
    List<PaperResponse> findAllApprovedPapers();

    List<PaperResponse> findApprovedPapers(String normalizedKeyword,
                                          String normalizedCategory,
                                          List<String> normalizedResearchAreas);

    List<PaperResponse> findMyPapers(UUID userId);

    Optional<PaperResponse> findApprovedPaperById(UUID paperId);

    Optional<PaperResponse> findPaperById(UUID paperId);

    boolean registerApprovedPaperView(UUID userId, UUID paperId);

    boolean registerApprovedPaperDownload(UUID userId, UUID paperId);

    List<PaperResponse.PaperAuthorResponse> findAuthorsByPaperId(UUID paperId);

    Map<UUID, List<PaperResponse.PaperAuthorResponse>> findAuthorsByPaperIds(List<UUID> paperIds);

    Optional<UUID> findUserIdByEmail(String email);

    boolean hasRole(UUID userId, String roleName);

    void upsertStudentProfile(UUID userId);

    void upsertLecturerProfile(UUID userId);

    Optional<String> findActiveResearchCategoryName(String researchAreaName);

    boolean existsApprovedPaperByPdfObjectKey(String objectKey);

    UUID createPaperWithMainAuthor(UUID userId,
                                   boolean lecturerAuthor,
                                   String title,
                                   String abstractText,
                                   String pdfUrl,
                                   int publicationYear,
                                   String journalConference,
                                   String researchArea,
                                   String category,
                                   String paperType);

    boolean isOwner(UUID paperId, UUID userId);

    int updatePaper(UUID paperId, String title, String abstractText, String pdfUrl, String researchArea, String paperType);

    boolean deletePaperByOwner(UUID paperId, UUID userId);
}
