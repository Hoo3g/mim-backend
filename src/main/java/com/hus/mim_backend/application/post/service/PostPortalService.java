package com.hus.mim_backend.application.post.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hus.mim_backend.application.port.output.PendingContentNotificationPort;
import com.hus.mim_backend.application.port.output.PostPortalRepository;
import com.hus.mim_backend.application.port.output.RecruitmentCategoryRepository;
import com.hus.mim_backend.application.port.output.UserRepository;
import com.hus.mim_backend.application.post.dto.PublicPostResponse;
import com.hus.mim_backend.application.post.dto.UpsertRecruitmentPostRequest;
import com.hus.mim_backend.application.post.usecase.PostPortalUseCase;
import com.hus.mim_backend.domain.auth.model.AccountStatus;
import com.hus.mim_backend.domain.auth.model.User;
import com.hus.mim_backend.domain.shared.ApprovalStatus;
import com.hus.mim_backend.domain.shared.DomainException;
import com.hus.mim_backend.shared.constants.CacheNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class PostPortalService implements PostPortalUseCase {
    private static final Logger log = LoggerFactory.getLogger(PostPortalService.class);

    private static final Set<String> ALLOWED_POST_TYPES = Set.of(
            "STUDENT_SEEKING_JOB",
            "STUDENT_SEEKING_INTERNSHIP",
            "COMPANY_RECRUITING_JOB",
            "COMPANY_RECRUITING_INTERNSHIP");

    private static final Set<String> ALLOWED_JOB_TYPES = Set.of("FULL_TIME", "PART_TIME", "CONTRACT", "INTERNSHIP");
    private static final Set<String> ALLOWED_POST_STATUSES = Set.of("OPEN", "CLOSED", "DRAFT");

    private final PostPortalRepository repository;
    private final UserRepository userRepository;
    private final RecruitmentCategoryRepository recruitmentCategoryRepository;
    private final PendingContentNotificationPort pendingContentNotificationPort;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PostPortalService(PostPortalRepository repository,
            UserRepository userRepository,
            RecruitmentCategoryRepository recruitmentCategoryRepository,
            PendingContentNotificationPort pendingContentNotificationPort) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.recruitmentCategoryRepository = recruitmentCategoryRepository;
        this.pendingContentNotificationPort = pendingContentNotificationPort;
    }

    @Override
    public List<PublicPostResponse> getMyPosts(String email) {
        UUID userId = resolveUserId(email);
        return repository.findPostsByAuthor(userId);
    }

    @Override
    public Optional<PublicPostResponse> getMyPostById(String email, UUID postId) {
        UUID userId = resolveUserId(email);
        return repository.findPostByIdForAuthor(postId, userId);
    }

    @Override
    public Optional<PublicPostResponse> getPostByIdForViewer(UUID postId, String viewerEmail) {
        UUID viewerId = null;
        if (StringUtils.hasText(viewerEmail)) {
            viewerId = resolveUserId(viewerEmail);
        }
        return repository.findPostByIdForViewer(postId, viewerId);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.PUBLIC_POSTS, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.PUBLIC_POST_DETAILS, allEntries = true)
    })
    public PublicPostResponse createPost(String email, UpsertRecruitmentPostRequest request) {
        if (request == null) {
            throw new DomainException("Request body is required");
        }

        UUID userId = resolveUserId(email);
        ensureVerifiedPublisher(userId);
        String role = resolveRole(userId);
        ensureStudentCanCreatePost(userId, role);
        normalizeAndValidate(request, role);
        String targetApprovalStatus = resolveCreateApprovalStatus(role);

        String displayInfoJson = toJsonOrNull(request.getDisplayInfo());
        String tagsCsv = toCsvOrNull(request.getTags());
        UUID createdId = repository.createPost(userId, request, displayInfoJson, tagsCsv, targetApprovalStatus);
        repository.replaceLinkedResearchPapers(createdId, extractPaperIds(request.getResearchPaperLinks()));

        if (ApprovalStatus.PENDING.name().equals(targetApprovalStatus)
                && TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    sendPendingNotification(request.getTitle(), email, createdId);
                }
            });
        } else if (ApprovalStatus.PENDING.name().equals(targetApprovalStatus)) {
            sendPendingNotification(request.getTitle(), email, createdId);
        }

        return repository.findPostByIdForAuthor(createdId, userId)
                .orElseThrow(() -> new DomainException("Unable to load created post"));
    }

    private void sendPendingNotification(String title, String authorEmail, UUID postId) {
        try {
            pendingContentNotificationPort.notifyNewPendingContent("POST", postId.toString(), title, authorEmail);
        } catch (RuntimeException ex) {
            log.warn("Failed to send pending content notification for post {}: {}", postId, ex.getMessage());
        }
    }

    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.PUBLIC_POSTS, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.PUBLIC_POST_DETAILS, allEntries = true)
    })
    public PublicPostResponse updatePost(String email, UUID postId, UpsertRecruitmentPostRequest request) {
        if (request == null) {
            throw new DomainException("Request body is required");
        }

        UUID userId = resolveUserId(email);
        ensureVerifiedPublisher(userId);
        String role = resolveRole(userId);
        normalizeAndValidate(request, role);
        PublicPostResponse existingPost = repository.findPostByIdForAuthor(postId, userId)
                .orElseThrow(() -> new DomainException("Post not found or you do not have permission to update"));
        String targetApprovalStatus = resolveUpdateApprovalStatus(role, existingPost.getApprovalStatus());

        String displayInfoJson = toJsonOrNull(request.getDisplayInfo());
        String tagsCsv = toCsvOrNull(request.getTags());
        boolean updated = repository.updatePostByAuthor(
                postId,
                userId,
                request,
                displayInfoJson,
                tagsCsv,
                targetApprovalStatus);
        if (!updated) {
            throw new DomainException("Post not found or you do not have permission to update");
        }

        repository.replaceLinkedResearchPapers(postId, extractPaperIds(request.getResearchPaperLinks()));
        return repository.findPostByIdForAuthor(postId, userId)
                .orElseThrow(() -> new DomainException("Unable to load updated post"));
    }

    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.PUBLIC_POSTS, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.PUBLIC_POST_DETAILS, allEntries = true)
    })
    public boolean deletePost(String email, UUID postId) {
        UUID userId = resolveUserId(email);
        return repository.deletePostByAuthor(postId, userId);
    }

    private UUID resolveUserId(String email) {
        if (!StringUtils.hasText(email)) {
            throw new DomainException("Authentication required");
        }
        return repository.findUserIdByEmail(email)
                .orElseThrow(() -> new DomainException("Authenticated user is not found"));
    }

    private String resolveRole(UUID userId) {
        return repository.findPrimaryRole(userId)
                .orElse("STUDENT")
                .toUpperCase(Locale.ROOT);
    }

    private void ensureVerifiedPublisher(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new DomainException("Authenticated user is not found"));
        if (user.getStatus() != AccountStatus.APPROVED) {
            throw new DomainException("Email chưa được xác thực. Tài khoản chỉ được xem nội dung cho tới khi hoàn tất xác thực email.");
        }
    }

    private void ensureStudentCanCreatePost(UUID userId, String role) {
        if (!"STUDENT".equals(role)) {
            return;
        }

        if (!repository.findPostsByAuthor(userId).isEmpty()) {
            throw new DomainException("Tài khoản sinh viên chỉ được tạo duy nhất 1 bài tuyển dụng. Bạn hãy chỉnh sửa bài đã có.");
        }
    }

    private String resolveCreateApprovalStatus(String role) {
        if ("COMPANY".equals(role)) {
            return ApprovalStatus.APPROVED.name();
        }
        return ApprovalStatus.PENDING.name();
    }

    private String resolveUpdateApprovalStatus(String role, String currentApprovalStatus) {
        if ("COMPANY".equals(role)) {
            return ApprovalStatus.APPROVED.name();
        }
        if (ApprovalStatus.APPROVED.name().equalsIgnoreCase(trimToNull(currentApprovalStatus))) {
            return ApprovalStatus.APPROVED.name();
        }
        return ApprovalStatus.PENDING.name();
    }

    private void normalizeAndValidate(UpsertRecruitmentPostRequest request, String role) {
        request.setTitle(trimToNull(request.getTitle()));
        request.setDescription(trimToNull(request.getDescription()));
        request.setPostType(normalizeUpper(trimToNull(request.getPostType())));
        request.setJobType(normalizeUpper(trimToNull(request.getJobType())));
        request.setRequirements(trimToNull(request.getRequirements()));
        request.setBenefits(trimToNull(request.getBenefits()));
        request.setAchievements(trimToNull(request.getAchievements()));
        request.setLocation(trimToNull(request.getLocation()));
        request.setSalaryRange(trimToNull(request.getSalaryRange()));
        request.setContactEmail(trimToNull(request.getContactEmail()));
        request.setContactPhone(trimToNull(request.getContactPhone()));
        request.setStudentCvUrl(trimToNull(request.getStudentCvUrl()));
        request.setStatus(normalizeUpper(trimToNull(request.getStatus())));
        request.setTags(normalizeTags(request.getTags()));
        validateRecruitmentCategories(request.getTags());
        request.setDisplayInfo(normalizeDisplayInfo(request.getDisplayInfo()));

        if (!StringUtils.hasText(request.getTitle()) || !StringUtils.hasText(request.getDescription())) {
            throw new DomainException("Title and description are required");
        }

        if (!StringUtils.hasText(request.getPostType()) || !ALLOWED_POST_TYPES.contains(request.getPostType())) {
            throw new DomainException("Invalid postType");
        }

        if (!StringUtils.hasText(request.getJobType())) {
            request.setJobType(defaultJobTypeByPostType(request.getPostType()));
        }
        if (!ALLOWED_JOB_TYPES.contains(request.getJobType())) {
            throw new DomainException("Invalid jobType");
        }

        if (!StringUtils.hasText(request.getStatus())) {
            request.setStatus("OPEN");
        }
        if (!ALLOWED_POST_STATUSES.contains(request.getStatus())) {
            throw new DomainException("Invalid status");
        }

        if (!"STUDENT".equals(role) && !"COMPANY".equals(role)) {
            throw new DomainException("Only student or company accounts can create/update recruitment posts");
        }

        if ("STUDENT".equals(role) && !request.getPostType().startsWith("STUDENT_")) {
            throw new DomainException("Student account can only create STUDENT_* posts");
        }
        if ("COMPANY".equals(role) && !request.getPostType().startsWith("COMPANY_")) {
            throw new DomainException("Company account can only create COMPANY_* posts");
        }
    }

    private String defaultJobTypeByPostType(String postType) {
        if (postType != null && postType.endsWith("INTERNSHIP")) {
            return "INTERNSHIP";
        }
        return "FULL_TIME";
    }

    private List<String> normalizeTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return null;
        }

        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String tag : tags) {
            String value = trimToNull(tag);
            if (value != null) {
                normalized.add(value);
            }
        }
        if (normalized.isEmpty()) {
            return null;
        }
        return new ArrayList<>(normalized);
    }

    private void validateRecruitmentCategories(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return;
        }

        Set<String> activeCategories = recruitmentCategoryRepository.findActiveRecruitmentCategoryNames(tags).stream()
                .map((item) -> item.trim().toLowerCase(Locale.ROOT))
                .collect(java.util.stream.Collectors.toSet());

        List<String> invalidCategories = tags.stream()
                .map(String::trim)
                .filter((item) -> !item.isBlank())
                .filter((item) -> !activeCategories.contains(item.toLowerCase(Locale.ROOT)))
                .toList();

        if (!invalidCategories.isEmpty()) {
            throw new DomainException("Invalid recruitment categories: " + String.join(", ", invalidCategories));
        }
    }

    private Map<String, Object> normalizeDisplayInfo(Map<String, Object> displayInfo) {
        if (displayInfo == null || displayInfo.isEmpty()) {
            return null;
        }

        Map<String, Object> normalized = new java.util.LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : displayInfo.entrySet()) {
            String key = trimToNull(entry.getKey());
            if (key == null) {
                continue;
            }

            Object value = entry.getValue();
            if (value instanceof String strValue) {
                String trimmed = trimToNull(strValue);
                if (trimmed != null) {
                    normalized.put(key, trimmed);
                }
                continue;
            }

            if (value instanceof Number || value instanceof Boolean) {
                normalized.put(key, value);
            }
        }

        return normalized.isEmpty() ? null : normalized;
    }

    private String toJsonOrNull(Map<String, Object> value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new DomainException("Invalid displayInfo payload");
        }
    }

    private String toCsvOrNull(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return String.join(",", values);
    }

    private List<UUID> extractPaperIds(List<UpsertRecruitmentPostRequest.ResearchPaperLinkItem> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }

        LinkedHashSet<UUID> ids = new LinkedHashSet<>();
        for (UpsertRecruitmentPostRequest.ResearchPaperLinkItem item : items) {
            if (item == null) {
                continue;
            }
            String idValue = trimToNull(item.getId());
            if (idValue == null) {
                continue;
            }
            try {
                ids.add(UUID.fromString(idValue));
            } catch (IllegalArgumentException ignored) {
                // Ignore invalid id values from client payload.
            }
        }
        return ids.isEmpty() ? List.of() : new ArrayList<>(ids);
    }

    private String normalizeUpper(String value) {
        if (value == null) {
            return null;
        }
        return value.toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
