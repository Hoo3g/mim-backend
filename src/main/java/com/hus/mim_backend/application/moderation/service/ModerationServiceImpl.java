package com.hus.mim_backend.application.moderation.service;

import com.hus.mim_backend.application.moderation.dto.ModerationRequest;
import com.hus.mim_backend.application.moderation.usecase.ModerationUseCase;
import com.hus.mim_backend.application.port.output.ModerationLogRepository;
import com.hus.mim_backend.application.port.output.PostRepository;
import com.hus.mim_backend.application.port.output.ResearchPaperRepository;
import com.hus.mim_backend.application.port.output.UserRepository;
import com.hus.mim_backend.domain.auth.model.AccountStatus;
import com.hus.mim_backend.domain.moderation.model.ModerationLog;
import com.hus.mim_backend.domain.post.model.Post;
import com.hus.mim_backend.domain.research.model.ResearchPaper;
import com.hus.mim_backend.domain.shared.DomainException;
import com.hus.mim_backend.infrastructure.config.CacheNames;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;

import java.util.Locale;
import java.util.UUID;

/**
 * Service orchestrating Moderation use cases
 */
public class ModerationServiceImpl implements ModerationUseCase {

    private final PostRepository postRepository;
    private final ResearchPaperRepository paperRepository;
    private final UserRepository userRepository;
    private final ModerationLogRepository logRepository;

    public ModerationServiceImpl(PostRepository postRepository,
            ResearchPaperRepository paperRepository,
            UserRepository userRepository,
            ModerationLogRepository logRepository) {
        this.postRepository = postRepository;
        this.paperRepository = paperRepository;
        this.userRepository = userRepository;
        this.logRepository = logRepository;
    }

    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.PUBLIC_POSTS, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.PUBLIC_POST_DETAILS, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.PUBLIC_RESEARCH_PAPERS, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.PUBLIC_RESEARCH_PAPER_DETAILS, allEntries = true)
    })
    public void approveContent(UUID moderatorId, ModerationRequest request) {
        if (moderatorId == null) {
            throw new DomainException("moderatorId is required");
        }
        ModerationLog.TargetType targetType = parseTargetType(request);
        UUID targetId = requireTargetId(request);
        String comment = normalizeNullableComment(request.getComment());

        switch (targetType) {
            case POST -> {
                Post post = postRepository.findById(targetId)
                        .orElseThrow(() -> new DomainException("Post not found"));
                post.approve(moderatorId, comment);
                postRepository.save(post);
            }
            case PAPER -> {
                ResearchPaper paper = paperRepository.findById(targetId)
                        .orElseThrow(() -> new DomainException("Paper not found"));
                paper.approve(moderatorId, comment);
                paperRepository.save(paper);
            }
            case USER -> {
                var user = userRepository.findById(targetId)
                        .orElseThrow(() -> new DomainException("User not found"));
                user.setStatus(AccountStatus.APPROVED);
                userRepository.save(user);
            }
        }

        logRepository.save(ModerationLog.createApproval(moderatorId, targetType, targetId, comment));
    }

    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.PUBLIC_POSTS, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.PUBLIC_POST_DETAILS, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.PUBLIC_RESEARCH_PAPERS, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.PUBLIC_RESEARCH_PAPER_DETAILS, allEntries = true)
    })
    public void rejectContent(UUID moderatorId, ModerationRequest request) {
        if (moderatorId == null) {
            throw new DomainException("moderatorId is required");
        }
        ModerationLog.TargetType targetType = parseTargetType(request);
        UUID targetId = requireTargetId(request);
        String comment = normalizeRequiredComment(request.getComment());

        switch (targetType) {
            case POST -> {
                Post post = postRepository.findById(targetId)
                        .orElseThrow(() -> new DomainException("Post not found"));
                post.reject(moderatorId, comment);
                postRepository.save(post);
            }
            case PAPER -> {
                ResearchPaper paper = paperRepository.findById(targetId)
                        .orElseThrow(() -> new DomainException("Paper not found"));
                paper.reject(moderatorId, comment);
                paperRepository.save(paper);
            }
            case USER -> {
                var user = userRepository.findById(targetId)
                        .orElseThrow(() -> new DomainException("User not found"));
                user.setStatus(AccountStatus.BLOCKED);
                userRepository.save(user);
            }
        }

        logRepository.save(ModerationLog.createRejection(moderatorId, targetType, targetId, comment));
    }

    private ModerationLog.TargetType parseTargetType(ModerationRequest request) {
        if (request == null || request.getTargetType() == null || request.getTargetType().isBlank()) {
            throw new DomainException("targetType is required");
        }

        String normalized = request.getTargetType().trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "POST" -> ModerationLog.TargetType.POST;
            case "PAPER", "RESEARCH_PAPER" -> ModerationLog.TargetType.PAPER;
            case "USER" -> ModerationLog.TargetType.USER;
            default -> throw new DomainException("Unsupported targetType: " + normalized);
        };
    }

    private UUID requireTargetId(ModerationRequest request) {
        if (request == null || request.getTargetId() == null) {
            throw new DomainException("targetId is required");
        }
        return request.getTargetId();
    }

    private String normalizeNullableComment(String comment) {
        if (comment == null) {
            return null;
        }
        String normalized = comment.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeRequiredComment(String comment) {
        String normalized = normalizeNullableComment(comment);
        if (normalized == null) {
            throw new DomainException("Rejection comment is required");
        }
        return normalized;
    }
}
