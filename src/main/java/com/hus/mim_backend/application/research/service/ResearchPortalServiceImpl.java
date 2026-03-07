package com.hus.mim_backend.application.research.service;

import com.hus.mim_backend.application.port.output.ResearchPortalRepository;
import com.hus.mim_backend.application.research.dto.PaperResponse;
import com.hus.mim_backend.application.research.dto.UpsertPaperRequest;
import com.hus.mim_backend.application.research.usecase.ManageResearchPortalUseCase;
import com.hus.mim_backend.domain.shared.DomainException;
import org.springframework.util.StringUtils;

import java.time.Year;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Application service for research portal APIs.
 */
public class ResearchPortalServiceImpl implements ManageResearchPortalUseCase {
    private static final String ROLE_LECTURER = "LECTURER";
    private static final String DEFAULT_JOURNAL = "MIM Draft";
    private static final String DEFAULT_PDF_URL = "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf";

    private final ResearchPortalRepository repository;

    public ResearchPortalServiceImpl(ResearchPortalRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<PaperResponse> getAllApprovedPapers() {
        List<PaperResponse> papers = repository.findAllApprovedPapers();
        papers.forEach(this::loadAuthors);
        return papers;
    }

    @Override
    public List<PaperResponse> getMyPapers(String currentUserEmail) {
        UUID userId = resolveCurrentUserId(currentUserEmail);
        List<PaperResponse> papers = repository.findMyPapers(userId);
        papers.forEach(this::loadAuthors);
        return papers;
    }

    @Override
    public Optional<PaperResponse> getApprovedPaperById(UUID paperId) {
        Optional<PaperResponse> paper = repository.findApprovedPaperById(paperId);
        paper.ifPresent(this::loadAuthors);
        return paper;
    }

    @Override
    public PaperResponse createPaper(String currentUserEmail, UpsertPaperRequest request) {
        validateUpsertRequest(request);

        UUID userId = resolveCurrentUserId(currentUserEmail);
        boolean isLecturer = repository.hasRole(userId, ROLE_LECTURER);
        if (isLecturer) {
            repository.upsertLecturerProfile(userId);
        } else {
            repository.upsertStudentProfile(userId);
        }

        String normalizedTitle = request.getTitle().trim();
        String normalizedAbstract = request.getAbstractText().trim();
        String normalizedPdfUrl = normalizePdfUrl(request.getPdfUrl());
        String normalizedResearchArea = resolveActiveResearchArea(request.getResearchArea());
        String category = isLecturer ? ROLE_LECTURER : "STUDENT";

        UUID paperId = repository.createPaperWithMainAuthor(
                userId,
                isLecturer,
                normalizedTitle,
                normalizedAbstract,
                normalizedPdfUrl,
                Year.now().getValue(),
                DEFAULT_JOURNAL,
                normalizedResearchArea,
                category);

        PaperResponse response = repository.findPaperById(paperId)
                .orElseThrow(() -> new DomainException("Research paper not found"));
        loadAuthors(response);
        return response;
    }

    @Override
    public UpdatePaperResult updatePaper(String currentUserEmail, UUID paperId, UpsertPaperRequest request) {
        validateUpsertRequest(request);

        UUID userId = resolveCurrentUserId(currentUserEmail);
        if (!repository.isOwner(paperId, userId)) {
            return UpdatePaperResult.forbidden();
        }

        String normalizedResearchArea = resolveActiveResearchArea(request.getResearchArea());
        int updated = repository.updatePaper(
                paperId,
                request.getTitle().trim(),
                request.getAbstractText().trim(),
                normalizePdfUrl(request.getPdfUrl()),
                normalizedResearchArea);
        if (updated == 0) {
            return UpdatePaperResult.notFound();
        }

        PaperResponse response = repository.findPaperById(paperId)
                .orElseThrow(() -> new DomainException("Research paper not found"));
        loadAuthors(response);
        return UpdatePaperResult.success(response);
    }

    private UUID resolveCurrentUserId(String email) {
        if (!StringUtils.hasText(email)) {
            throw new DomainException("Authentication required");
        }

        return repository.findUserIdByEmail(email.trim())
                .orElseThrow(() -> new DomainException("Authenticated user is not found"));
    }

    private void validateUpsertRequest(UpsertPaperRequest request) {
        if (request == null
                || !StringUtils.hasText(request.getTitle())
                || !StringUtils.hasText(request.getAbstractText())
                || !StringUtils.hasText(request.getResearchArea())) {
            throw new DomainException("Title, abstract and researchArea are required");
        }
    }

    private String normalizePdfUrl(String pdfUrl) {
        if (pdfUrl == null || pdfUrl.isBlank()) {
            return DEFAULT_PDF_URL;
        }
        return pdfUrl.trim();
    }

    private void loadAuthors(PaperResponse paper) {
        paper.setAuthors(repository.findAuthorsByPaperId(paper.getId()));
    }

    private String resolveActiveResearchArea(String researchArea) {
        if (!StringUtils.hasText(researchArea)) {
            throw new DomainException("researchArea is required");
        }
        return repository.findActiveResearchCategoryName(researchArea.trim())
                .orElseThrow(() -> new DomainException("Research area is invalid or inactive"));
    }
}
