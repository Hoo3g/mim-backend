package com.hus.mim_backend.application.post.service;

import com.hus.mim_backend.application.port.output.ApplicationRepository;
import com.hus.mim_backend.application.port.output.PostRepository;
import com.hus.mim_backend.application.port.output.SavedPostRepository;
import com.hus.mim_backend.application.post.dto.ApplicationRequest;
import com.hus.mim_backend.application.post.dto.ApplicationResponse;
import com.hus.mim_backend.application.post.dto.CreatePostRequest;
import com.hus.mim_backend.application.post.dto.PostResponse;
import com.hus.mim_backend.application.post.dto.UpdatePostRequest;
import com.hus.mim_backend.application.post.usecase.ApplyToPostUseCase;
import com.hus.mim_backend.application.post.usecase.ManagePostUseCase;
import com.hus.mim_backend.domain.post.model.Application;
import com.hus.mim_backend.domain.post.model.Post;
import com.hus.mim_backend.domain.shared.DomainException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service orchestrating Post and Application use cases
 */
public class PostServiceImpl implements ManagePostUseCase, ApplyToPostUseCase {

    private final PostRepository postRepository;
    private final ApplicationRepository applicationRepository;
    private final SavedPostRepository savedPostRepository;

    public PostServiceImpl(PostRepository postRepository,
            ApplicationRepository applicationRepository,
            SavedPostRepository savedPostRepository) {
        this.postRepository = postRepository;
        this.applicationRepository = applicationRepository;
        this.savedPostRepository = savedPostRepository;
    }

    @Override
    public PostResponse createPost(UUID authorId, CreatePostRequest request) {
        if (authorId == null) {
            throw new DomainException("authorId is required");
        }
        if (request == null) {
            throw new DomainException("request is required");
        }

        String title = normalizeNullableText(request.getTitle());
        String description = normalizeNullableText(request.getDescription());
        Post.PostType postType = parsePostType(request.getPostType());
        Post.JobType jobType = parseJobType(request.getJobType(), postType);

        Post post = Post.createNew(authorId, title, description, postType, jobType);
        post.setRequirements(normalizeNullableText(request.getRequirements()));
        post.setBenefits(normalizeNullableText(request.getBenefits()));
        post.setLocation(normalizeNullableText(request.getLocation()));
        post.setSalaryRange(normalizeNullableText(request.getSalaryRange()));
        post.setTags(normalizeTags(request.getTags()));

        Post saved = postRepository.save(post);
        return toPostResponse(saved);
    }

    @Override
    public PostResponse updatePost(UUID postId, UpdatePostRequest request) {
        if (postId == null) {
            throw new DomainException("postId is required");
        }
        if (request == null) {
            throw new DomainException("request is required");
        }

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new DomainException("Post not found"));

        String title = normalizeNullableText(request.getTitle());
        if (title != null) {
            post.setTitle(title);
        }

        String description = normalizeNullableText(request.getDescription());
        if (description != null) {
            post.setDescription(description);
        }

        String status = normalizeNullableText(request.getStatus());
        if (status != null) {
            post.setStatus(parsePostStatus(status));
        }

        if (request.getTags() != null) {
            post.setTags(normalizeTags(request.getTags()));
        }
        post.setUpdatedAt(LocalDateTime.now());

        Post saved = postRepository.save(post);
        return toPostResponse(saved);
    }

    @Override
    public void deletePost(UUID postId) {
        if (postId == null) {
            throw new DomainException("postId is required");
        }
        postRepository.deleteById(postId);
    }

    @Override
    public PostResponse getPost(UUID postId) {
        if (postId == null) {
            throw new DomainException("postId is required");
        }
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new DomainException("Post not found"));
        return toPostResponse(post);
    }

    @Override
    public List<PostResponse> searchPosts(String keyword, List<String> tags) {
        String normalizedKeyword = normalizeNullableText(keyword);
        List<String> normalizedTags = normalizeTags(tags);

        List<Post> result;
        if (normalizedKeyword == null && (normalizedTags == null || normalizedTags.isEmpty())) {
            result = postRepository.findByStatus(Post.PostStatus.OPEN.name());
        } else if (normalizedKeyword != null && (normalizedTags == null || normalizedTags.isEmpty())) {
            result = postRepository.searchByTitleOrDescription(normalizedKeyword);
        } else if (normalizedKeyword == null) {
            result = postRepository.findByTags(normalizedTags);
        } else {
            List<Post> keywordMatches = postRepository.searchByTitleOrDescription(normalizedKeyword);
            Map<UUID, Post> tagMatches = postRepository.findByTags(normalizedTags).stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.toMap(Post::getId, post -> post, (left, right) -> left));
            result = keywordMatches.stream()
                    .filter(post -> post != null && post.getId() != null && tagMatches.containsKey(post.getId()))
                    .toList();
        }

        return deduplicatePosts(result).stream()
                .map(this::toPostResponse)
                .toList();
    }

    @Override
    public List<PostResponse> getMyPosts(UUID authorId) {
        if (authorId == null) {
            throw new DomainException("authorId is required");
        }
        return postRepository.findByAuthorId(authorId).stream()
                .map(this::toPostResponse)
                .toList();
    }

    @Override
    public ApplicationResponse apply(UUID applicantId, UUID postId, ApplicationRequest request) {
        if (applicantId == null) {
            throw new DomainException("applicantId is required");
        }
        if (postId == null) {
            throw new DomainException("postId is required");
        }
        if (applicationRepository.existsByPostIdAndApplicantId(postId, applicantId)) {
            throw new DomainException("You already applied to this post");
        }

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new DomainException("Post not found"));
        if (post.getStatus() == Post.PostStatus.CLOSED) {
            throw new DomainException("Post is closed");
        }
        if (post.getApprovalStatus() != Post.ApprovalStatus.APPROVED) {
            throw new DomainException("Post is not approved for applications");
        }

        Application application = Application.builder()
                .id(UUID.randomUUID())
                .postId(postId)
                .applicantId(applicantId)
                .status(Application.ApplicationStatus.PENDING)
                .message(request == null ? null : normalizeNullableText(request.getMessage()))
                .cvUrl(request == null ? null : normalizeNullableText(request.getCvUrl()))
                .createdAt(LocalDateTime.now())
                .build();

        Application saved = applicationRepository.save(application);
        return toApplicationResponse(saved, post.getTitle());
    }

    @Override
    public void updateApplicationStatus(UUID applicationId, String status) {
        if (applicationId == null) {
            throw new DomainException("applicationId is required");
        }

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new DomainException("Application not found"));

        String normalizedStatus = normalizeRequiredUpper(status, "status is required");
        switch (normalizedStatus) {
            case "REVIEWED" -> application.markAsReviewed();
            case "ACCEPT", "ACCEPTED", "APPROVED" -> application.accept();
            case "REJECT", "REJECTED" -> application.reject();
            case "PENDING" -> application.setStatus(Application.ApplicationStatus.PENDING);
            default -> throw new DomainException("Unsupported application status: " + normalizedStatus);
        }

        applicationRepository.save(application);
    }

    @Override
    public List<ApplicationResponse> getApplicationsForPost(UUID postId) {
        if (postId == null) {
            throw new DomainException("postId is required");
        }
        String postTitle = postRepository.findById(postId)
                .map(Post::getTitle)
                .orElse(null);

        return applicationRepository.findByPostId(postId).stream()
                .map(application -> toApplicationResponse(application, postTitle))
                .toList();
    }

    @Override
    public List<ApplicationResponse> getMyApplications(UUID applicantId) {
        if (applicantId == null) {
            throw new DomainException("applicantId is required");
        }
        Map<UUID, String> postTitleById = new LinkedHashMap<>();
        return applicationRepository.findByApplicantId(applicantId).stream()
                .map((application) -> {
                    String postTitle = postTitleById.computeIfAbsent(application.getPostId(), (postId) -> postRepository
                            .findById(postId)
                            .map(Post::getTitle)
                            .orElse(null));
                    return toApplicationResponse(application, postTitle);
                })
                .toList();
    }

    private Post.PostType parsePostType(String value) {
        String normalized = normalizeRequiredUpper(value, "postType is required");
        try {
            return Post.PostType.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new DomainException("Invalid postType: " + normalized);
        }
    }

    private Post.JobType parseJobType(String value, Post.PostType postType) {
        String normalized = normalizeNullableText(value);
        if (normalized == null) {
            if (postType != null && postType.name().endsWith("INTERNSHIP")) {
                return Post.JobType.INTERNSHIP;
            }
            return Post.JobType.FULL_TIME;
        }
        try {
            return Post.JobType.valueOf(normalized.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new DomainException("Invalid jobType: " + normalized);
        }
    }

    private Post.PostStatus parsePostStatus(String value) {
        String normalized = normalizeRequiredUpper(value, "status is required");
        try {
            return Post.PostStatus.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new DomainException("Invalid post status: " + normalized);
        }
    }

    private PostResponse toPostResponse(Post post) {
        PostResponse response = new PostResponse();
        response.setId(post.getId());
        response.setTitle(post.getTitle());
        response.setDescription(post.getDescription());
        response.setPostType(post.getPostType() == null ? null : post.getPostType().name());
        response.setStatus(post.getStatus() == null ? null : post.getStatus().name());
        response.setCreatedAt(post.getCreatedAt());
        return response;
    }

    private ApplicationResponse toApplicationResponse(Application application, String postTitle) {
        ApplicationResponse response = new ApplicationResponse();
        response.setId(application.getId());
        response.setPostId(application.getPostId());
        response.setPostTitle(postTitle);
        response.setApplicantId(application.getApplicantId());
        response.setApplicantName(null);
        response.setStatus(application.getStatus() == null ? null : application.getStatus().name());
        response.setMessage(application.getMessage());
        response.setCvUrl(application.getCvUrl());
        response.setCreatedAt(application.getCreatedAt());
        return response;
    }

    private List<Post> deduplicatePosts(List<Post> posts) {
        if (posts == null || posts.isEmpty()) {
            return List.of();
        }
        Map<UUID, Post> byId = new LinkedHashMap<>();
        for (Post post : posts) {
            if (post == null || post.getId() == null) {
                continue;
            }
            byId.putIfAbsent(post.getId(), post);
        }
        return new ArrayList<>(byId.values());
    }

    private List<String> normalizeTags(List<String> tags) {
        if (tags == null) {
            return null;
        }
        List<String> normalized = tags.stream()
                .map(this::normalizeNullableText)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        return normalized.isEmpty() ? List.of() : normalized;
    }

    private String normalizeRequiredUpper(String value, String message) {
        String normalized = normalizeNullableText(value);
        if (normalized == null) {
            throw new DomainException(message);
        }
        return normalized.toUpperCase(Locale.ROOT);
    }

    private String normalizeNullableText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
