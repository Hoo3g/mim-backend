package com.hus.mim_backend.application.research.service;

import com.hus.mim_backend.application.port.output.PendingContentNotificationPort;
import com.hus.mim_backend.application.port.output.UserRepository;
import com.hus.mim_backend.application.port.output.ResearchPortalRepository;
import com.hus.mim_backend.application.research.dto.PaperResponse;
import com.hus.mim_backend.application.research.dto.StudentAuthorLookupResponse;
import com.hus.mim_backend.application.research.dto.UpsertPaperRequest;
import com.hus.mim_backend.application.research.usecase.ManageResearchPortalUseCase;
import com.hus.mim_backend.domain.auth.model.AccountStatus;
import com.hus.mim_backend.domain.auth.model.User;
import com.hus.mim_backend.domain.shared.DomainException;
import com.hus.mim_backend.shared.constants.CacheNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.Normalizer;
import java.time.Year;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Application service for research portal APIs.
 */
public class ResearchPortalServiceImpl implements ManageResearchPortalUseCase {
    private static final Logger log = LoggerFactory.getLogger(ResearchPortalServiceImpl.class);

    private static final String ROLE_LECTURER = "LECTURER";
    private static final String ROLE_STUDENT = "STUDENT";
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String PAPER_TYPE_SCIENTIFIC_RESEARCH = "SCIENTIFIC_RESEARCH";
    private static final String PAPER_TYPE_GRADUATION_THESIS = "GRADUATION_THESIS";
    private static final String DEFAULT_JOURNAL = "MIM Draft";
    private static final String PUBLIC_RESEARCH_PDF_PREFIX = "/api/public/storage/research-pdfs/";
    private static final String LEGACY_RESEARCH_PDF_PREFIX = "/api/v1/storage/research-pdfs/";
    private static final int STUDENT_AUTHOR_SEARCH_LIMIT = 8;

    private final ResearchPortalRepository repository;
    private final UserRepository userRepository;
    private final PendingContentNotificationPort pendingContentNotificationPort;

    public ResearchPortalServiceImpl(ResearchPortalRepository repository,
            UserRepository userRepository,
            PendingContentNotificationPort pendingContentNotificationPort) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.pendingContentNotificationPort = pendingContentNotificationPort;
    }

    @Override
    @Cacheable(cacheNames = CacheNames.PUBLIC_RESEARCH_PAPERS,
            key = "T(com.hus.mim_backend.shared.constants.CacheKeys).singleton()",
            sync = true)
    public List<PaperResponse> getAllApprovedPapers() {
        List<PaperResponse> papers = repository.findAllApprovedPapers();
        loadAuthors(papers);
        return papers;
    }

    @Override
    @Cacheable(cacheNames = CacheNames.PUBLIC_RESEARCH_PAPERS,
            key = "T(com.hus.mim_backend.shared.constants.CacheKeys).queryKey(#keyword, #category, #researchAreas)",
            sync = true)
    public List<PaperResponse> getAllApprovedPapers(String keyword, String category, List<String> researchAreas) {
        String normalizedKeyword = normalize(keyword);
        String normalizedCategory = normalize(category);
        List<String> normalizedResearchAreas = normalizeDistinct(researchAreas);

        List<PaperResponse> papers = repository.findApprovedPapers(
                normalizedKeyword,
                normalizedCategory,
                normalizedResearchAreas);
        loadAuthors(papers);
        return papers;
    }

    @Override
    public List<PaperResponse> getMyPapers(String currentUserEmail) {
        UUID userId = resolveCurrentUserId(currentUserEmail);
        List<PaperResponse> papers = repository.findMyPapers(userId);
        loadAuthors(papers);
        return papers;
    }

    @Override
    public List<StudentAuthorLookupResponse> searchStudentAuthors(String keyword) {
        String normalizedKeyword = normalizeStudentCodeKeyword(keyword);
        if (!StringUtils.hasText(normalizedKeyword) || normalizedKeyword.length() < 2) {
            return List.of();
        }
        return repository.searchStudentAuthorsByStudentCode(normalizedKeyword, STUDENT_AUTHOR_SEARCH_LIMIT);
    }

    @Override
    @Cacheable(cacheNames = CacheNames.PUBLIC_RESEARCH_PAPER_DETAILS,
            key = "T(com.hus.mim_backend.shared.constants.CacheKeys).idKey(#paperId)",
            sync = true)
    public Optional<PaperResponse> getApprovedPaperById(UUID paperId) {
        Optional<PaperResponse> paper = repository.findApprovedPaperById(paperId);
        paper.ifPresent(this::loadAuthors);
        return paper;
    }

    @Override
    public boolean trackApprovedPaperView(String currentUserEmail, UUID paperId) {
        UUID userId = resolveCurrentUserId(currentUserEmail);
        return repository.registerApprovedPaperView(userId, paperId);
    }

    @Override
    public boolean trackApprovedPaperDownload(String currentUserEmail, UUID paperId) {
        UUID userId = resolveCurrentUserId(currentUserEmail);
        return repository.registerApprovedPaperDownload(userId, paperId);
    }

    @Override
    public boolean canAccessResearchPdf(String objectKey) {
        if (!StringUtils.hasText(objectKey)) {
            return false;
        }
        return repository.existsApprovedPaperByPdfObjectKey(objectKey.trim());
    }

    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.PUBLIC_RESEARCH_PAPERS, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.PUBLIC_RESEARCH_PAPER_DETAILS, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.PUBLIC_POSTS, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.PUBLIC_POST_DETAILS, allEntries = true)
    })
    @Transactional
    public PaperResponse createPaper(String currentUserEmail, UpsertPaperRequest request) {
        validateUpsertRequest(request);
        validatePdfUrlIfProvided(request.getPdfUrl());

        UUID userId = resolveCurrentUserId(currentUserEmail);
        ensureVerifiedPublisher(userId);
        boolean isAdminPublisher = repository.hasRole(userId, ROLE_ADMIN);
        String authorRole = resolveResearchAuthorRole(userId, request.getCategory());
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
        String normalizedPaperType = resolvePaperType(request.getPaperType());
        int publicationYear = resolvePublicationYear(request.getPublicationYear());
        String journalConference = resolveJournalConference(request.getJournalConference());
        List<UUID> coAuthorStudentIds = resolveCoAuthorStudentIds(request.getCoAuthorStudentIds(), userId);
        String category = authorRole;
        String authorNameOverride = isAdminPublisher ? resolveAuthorNameOverride(userId, request.getAuthorName()) : null;
        String approvalStatus = isAdminPublisher ? "APPROVED" : "PENDING";
        UUID moderatorId = isAdminPublisher ? userId : null;

        UUID paperId = repository.createPaperWithMainAuthor(
                userId,
                isLecturer,
                authorNameOverride,
                normalizedTitle,
                normalizedAbstract,
                normalizedPdfUrl,
                publicationYear,
                journalConference,
                normalizedResearchArea,
                category,
                normalizedPaperType,
                approvalStatus,
                moderatorId);
        repository.replaceStudentCoAuthors(paperId, coAuthorStudentIds);

        PaperResponse response = repository.findPaperById(paperId)
                .orElseThrow(() -> new DomainException("Research paper not found"));
        loadAuthors(response);

        if (!"APPROVED".equals(approvalStatus) && TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    sendPendingNotification(request.getTitle(), currentUserEmail, paperId);
                }
            });
        } else if (!"APPROVED".equals(approvalStatus)) {
            sendPendingNotification(request.getTitle(), currentUserEmail, paperId);
        }

        return response;
    }

    private void sendPendingNotification(String title, String authorEmail, UUID paperId) {
        try {
            pendingContentNotificationPort.notifyNewPendingContent(
                    "PAPER", paperId.toString(), title, authorEmail);
        } catch (RuntimeException ex) {
            log.warn("Failed to send pending content notification for paper {}: {}", paperId, ex.getMessage());
        }
    }

    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.PUBLIC_RESEARCH_PAPERS, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.PUBLIC_RESEARCH_PAPER_DETAILS, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.PUBLIC_POSTS, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.PUBLIC_POST_DETAILS, allEntries = true)
    })
    @Transactional
    public UpdatePaperResult updatePaper(String currentUserEmail, UUID paperId, UpsertPaperRequest request) {
        validateUpsertRequest(request);
        validatePdfUrlIfProvided(request.getPdfUrl());

        UUID userId = resolveCurrentUserId(currentUserEmail);
        ensureVerifiedPublisher(userId);
        if (!repository.isOwner(paperId, userId)) {
            return UpdatePaperResult.forbidden();
        }

        String authorRole = resolveResearchAuthorRole(userId, request.getCategory());
        String normalizedResearchArea = resolveActiveResearchArea(request.getResearchArea());
        List<UUID> coAuthorStudentIds = resolveCoAuthorStudentIds(request.getCoAuthorStudentIds(), userId);
        int updated = repository.updatePaper(
                paperId,
                request.getTitle().trim(),
                request.getAbstractText().trim(),
                normalizePdfUrl(request.getPdfUrl()),
                normalizedResearchArea,
                resolvePaperType(request.getPaperType()),
                resolvePublicationYear(request.getPublicationYear()),
                resolveJournalConference(request.getJournalConference()),
                authorRole);
        if (updated == 0) {
            return UpdatePaperResult.notFound();
        }
        if (request.getCoAuthorStudentIds() != null) {
            repository.replaceStudentCoAuthors(paperId, coAuthorStudentIds);
        }

        PaperResponse response = repository.findPaperById(paperId)
                .orElseThrow(() -> new DomainException("Research paper not found"));
        loadAuthors(response);
        return UpdatePaperResult.success(response);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.PUBLIC_RESEARCH_PAPERS, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.PUBLIC_RESEARCH_PAPER_DETAILS, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.PUBLIC_POSTS, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.PUBLIC_POST_DETAILS, allEntries = true)
    })
    public boolean deletePaper(String currentUserEmail, UUID paperId) {
        UUID userId = resolveCurrentUserId(currentUserEmail);
        return repository.deletePaperByOwner(paperId, userId);
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

    private void validatePdfUrlIfProvided(String pdfUrl) {
        if (!StringUtils.hasText(pdfUrl)) {
            return;
        }

        if (!isAllowedResearchPdfUrl(pdfUrl.trim())) {
            throw new DomainException("PDF URL must point to MinIO storage endpoint.");
        }
    }

    private void ensureVerifiedPublisher(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new DomainException("Authenticated user is not found"));
        if (user.getStatus() != AccountStatus.APPROVED) {
            throw new DomainException("Email chưa được xác thực. Tài khoản chỉ được xem nội dung cho tới khi hoàn tất xác thực email.");
        }
    }

    private String resolveResearchAuthorRole(UUID userId, String requestedCategory) {
        if (repository.hasRole(userId, ROLE_LECTURER)) {
            return ROLE_LECTURER;
        }
        if (repository.hasRole(userId, ROLE_STUDENT)) {
            return ROLE_STUDENT;
        }
        if (repository.hasRole(userId, ROLE_ADMIN)) {
            return resolveRequestedCategory(requestedCategory);
        }
        throw new DomainException("Only student, lecturer or admin accounts can create research papers");
    }

    private String resolveRequestedCategory(String requestedCategory) {
        String normalized = normalize(requestedCategory);
        return switch (normalized) {
            case "", "student", "sinh vien", "sinhvien" -> ROLE_STUDENT;
            case "lecturer", "giang vien", "giangvien" -> ROLE_LECTURER;
            default -> throw new DomainException("category is invalid");
        };
    }

    private int resolvePublicationYear(Integer publicationYear) {
        int currentYear = Year.now().getValue();
        if (publicationYear == null) {
            return currentYear;
        }
        if (publicationYear < 1900 || publicationYear > currentYear + 1) {
            throw new DomainException("publicationYear is invalid");
        }
        return publicationYear;
    }

    private String resolveJournalConference(String journalConference) {
        if (!StringUtils.hasText(journalConference)) {
            return DEFAULT_JOURNAL;
        }
        return journalConference.trim();
    }

    private List<UUID> resolveCoAuthorStudentIds(List<String> requestedIds, UUID currentUserId) {
        if (requestedIds == null || requestedIds.isEmpty()) {
            return List.of();
        }

        LinkedHashSet<UUID> resolvedIds = new LinkedHashSet<>();
        for (String requestedId : requestedIds) {
            if (!StringUtils.hasText(requestedId)) {
                continue;
            }

            UUID parsedId;
            try {
                parsedId = UUID.fromString(requestedId.trim());
            } catch (IllegalArgumentException ex) {
                throw new DomainException("coAuthorStudentIds contains an invalid user id");
            }

            if (parsedId.equals(currentUserId)) {
                continue;
            }

            if (!userRepository.hasStudentRegistration(parsedId)) {
                throw new DomainException("coAuthorStudentIds contains an invalid student");
            }

            resolvedIds.add(parsedId);
        }

        return new ArrayList<>(resolvedIds);
    }

    private String resolveAuthorNameOverride(UUID userId, String requestedAuthorName) {
        if (StringUtils.hasText(requestedAuthorName)) {
            return requestedAuthorName.trim();
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new DomainException("Authenticated user is not found"));
        if (StringUtils.hasText(user.getFullName())) {
            return user.getFullName().trim();
        }
        if (user.getEmail() != null && StringUtils.hasText(user.getEmail().value())) {
            String email = user.getEmail().value().trim();
            int atIndex = email.indexOf('@');
            return atIndex > 0 ? email.substring(0, atIndex) : email;
        }
        return "Unknown";
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

    private void loadAuthors(List<PaperResponse> papers) {
        if (papers == null || papers.isEmpty()) {
            return;
        }

        List<UUID> paperIds = papers.stream()
                .map(PaperResponse::getId)
                .toList();
        Map<UUID, List<PaperResponse.PaperAuthorResponse>> authorsByPaperId = repository.findAuthorsByPaperIds(paperIds);
        papers.forEach((paper) -> paper.setAuthors(authorsByPaperId.getOrDefault(paper.getId(), List.of())));
    }

    private String resolveActiveResearchArea(String researchArea) {
        if (!StringUtils.hasText(researchArea)) {
            throw new DomainException("researchArea is required");
        }
        return repository.findActiveResearchCategoryName(researchArea.trim())
                .orElseThrow(() -> new DomainException("Research area is invalid or inactive"));
    }

    private String resolvePaperType(String paperType) {
        String normalized = normalize(paperType);
        return switch (normalized) {
            case "", "scientific_research", "scientific research", "Nghiên cứu khoa học" -> PAPER_TYPE_SCIENTIFIC_RESEARCH;
            case "graduation_thesis", "graduation thesis", "Khóa luận tốt nghiệp" -> PAPER_TYPE_GRADUATION_THESIS;
            default -> throw new DomainException("paperType is invalid");
        };
    }

    private List<String> normalizeDistinct(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }

        return values.stream()
                .filter(StringUtils::hasText)
                .flatMap((value) -> List.of(value.split(",")).stream())
                .map(String::trim)
                .map(this::normalize)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }

        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .trim();
        return normalized.replaceAll("\\s+", " ");
    }

    private String normalizeStudentCodeKeyword(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.trim().replaceAll("\\s+", "");
    }
}
