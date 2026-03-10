package com.hus.mim_backend.application.research.service;

import com.hus.mim_backend.application.port.output.ResearchPortalRepository;
import com.hus.mim_backend.application.research.dto.PaperResponse;
import com.hus.mim_backend.application.research.dto.UpsertPaperRequest;
import com.hus.mim_backend.application.research.usecase.ManageResearchPortalUseCase;
import com.hus.mim_backend.domain.shared.DomainException;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Year;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Application service for research portal APIs.
 */
public class ResearchPortalServiceImpl implements ManageResearchPortalUseCase {
    private static final String ROLE_LECTURER = "LECTURER";
    private static final String ROLE_STUDENT = "STUDENT";
    private static final String DEFAULT_JOURNAL = "MIM Draft";
    private static final String PUBLIC_RESEARCH_PDF_PREFIX = "/api/public/storage/research-pdfs/";
    private static final String LEGACY_RESEARCH_PDF_PREFIX = "/api/v1/storage/research-pdfs/";

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
        validatePdfUrlForCreate(request.getPdfUrl());

        UUID userId = resolveCurrentUserId(currentUserEmail);
        String authorRole = resolveResearchAuthorRole(userId);
        boolean isLecturer = ROLE_LECTURER.equals(authorRole);
        if (isLecturer) {
            repository.upsertLecturerProfile(userId);
        } else {
            repository.upsertStudentProfile(userId);
        }

        String normalizedTitle = request.getTitle().trim();
        String normalizedAbstract = request.getAbstractText().trim();
        String normalizedPdfUrl = normalizePdfUrl(request.getPdfUrl());
        String normalizedResearchArea = resolveActiveResearchArea(request.getResearchArea());
        String category = authorRole;

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
        validatePdfUrlIfProvided(request.getPdfUrl());

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
        if (!StringUtils.hasText(pdfUrl)) {
            return "";
        }
        return pdfUrl.trim();
    }

    private void validatePdfUrlForCreate(String pdfUrl) {
        if (!StringUtils.hasText(pdfUrl)) {
            throw new DomainException("PDF URL is required. Please upload PDF to MinIO first.");
        }
        validatePdfUrlIfProvided(pdfUrl);
    }

    private void validatePdfUrlIfProvided(String pdfUrl) {
        if (!StringUtils.hasText(pdfUrl)) {
            return;
        }

        if (!isAllowedResearchPdfUrl(pdfUrl.trim())) {
            throw new DomainException("PDF URL must point to MinIO storage endpoint.");
        }
    }

    private String resolveResearchAuthorRole(UUID userId) {
        if (repository.hasRole(userId, ROLE_LECTURER)) {
            return ROLE_LECTURER;
        }
        if (repository.hasRole(userId, ROLE_STUDENT)) {
            return ROLE_STUDENT;
        }
        throw new DomainException("Only student or lecturer accounts can create research papers");
    }

    private boolean isAllowedResearchPdfUrl(String value) {
        String path = extractPath(value);
        return path.startsWith(PUBLIC_RESEARCH_PDF_PREFIX) || path.startsWith(LEGACY_RESEARCH_PDF_PREFIX);
    }

    private String extractPath(String value) {
        if (value.startsWith("/")) {
            return value;
        }

        try {
            URI uri = new URI(value);
            String path = uri.getPath();
            if (!StringUtils.hasText(path)) {
                throw new DomainException("PDF URL must point to MinIO storage endpoint.");
            }
            return path;
        } catch (URISyntaxException ex) {
            throw new DomainException("PDF URL must point to MinIO storage endpoint.");
        }
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
