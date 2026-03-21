package com.hus.mim_backend.infrastructure.adapter.web.post;

import com.hus.mim_backend.application.post.dto.PublicPostResponse;
import com.hus.mim_backend.application.post.dto.UpsertRecruitmentPostRequest;
import com.hus.mim_backend.application.post.usecase.PostPortalUseCase;
import com.hus.mim_backend.application.post.usecase.QueryPublicPostsPageUseCase;
import com.hus.mim_backend.application.post.usecase.QueryPublicPostsUseCase;
import com.hus.mim_backend.application.shared.PagedResult;
import com.hus.mim_backend.domain.shared.AuthException;
import com.hus.mim_backend.shared.api.ApiResponse;
import com.hus.mim_backend.shared.constants.ApiEndpoints;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Public endpoints for recruitment/job posts.
 */
@RestController
@RequestMapping(ApiEndpoints.POSTS)
public class PostController {
    private final QueryPublicPostsUseCase queryPublicPostsUseCase;
    private final QueryPublicPostsPageUseCase queryPublicPostsPageUseCase;
    private final PostPortalUseCase postPortalUseCase;

    public PostController(QueryPublicPostsUseCase queryPublicPostsUseCase,
            QueryPublicPostsPageUseCase queryPublicPostsPageUseCase,
            PostPortalUseCase postPortalUseCase) {
        this.queryPublicPostsUseCase = queryPublicPostsUseCase;
        this.queryPublicPostsPageUseCase = queryPublicPostsPageUseCase;
        this.postPortalUseCase = postPortalUseCase;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PublicPostResponse>>> getPosts(
            @RequestParam(name = "q", required = false) String keyword,
            @RequestParam(name = "type", required = false) String type,
            @RequestParam(name = "specialization", required = false) List<String> specializations) {
        List<PublicPostResponse> posts = queryPublicPostsUseCase.getPosts(keyword, type, specializations);
        return ResponseEntity.ok(ApiResponse.success(posts, "Get posts successfully"));
    }

    @GetMapping(ApiEndpoints.POSTS_PAGED)
    public ResponseEntity<ApiResponse<PagedResult<PublicPostResponse>>> getPostsPaged(
            @RequestParam(name = "q", required = false) String keyword,
            @RequestParam(name = "type", required = false) String type,
            @RequestParam(name = "specialization", required = false) List<String> specializations,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        PagedResult<PublicPostResponse> posts = queryPublicPostsPageUseCase.getPostsPage(
                keyword,
                type,
                specializations,
                page,
                size);
        return ResponseEntity.ok(ApiResponse.success(posts, "Get paged posts successfully"));
    }

    @GetMapping(ApiEndpoints.POST_BY_ID)
    public ResponseEntity<ApiResponse<PublicPostResponse>> getPostById(
            @PathVariable UUID postId,
            Authentication authentication) {
        String viewerEmail = resolveOptionalAuthenticatedEmail(authentication);

        if (!StringUtils.hasText(viewerEmail)) {
            return queryPublicPostsUseCase.getPostById(postId)
                    .map(post -> ResponseEntity.ok(ApiResponse.success(post, "Get post successfully")))
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(ApiResponse.error("Post not found", "POST_NOT_FOUND")));
        }

        return postPortalUseCase.getPostByIdForViewer(postId, viewerEmail)
                .map(post -> ResponseEntity.ok(ApiResponse.success(post, "Get post successfully")))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Post not found", "POST_NOT_FOUND")));
    }

    @GetMapping(ApiEndpoints.POSTS_ME)
    public ResponseEntity<ApiResponse<List<PublicPostResponse>>> getMyPosts(Authentication authentication) {
        String email = resolveAuthenticatedEmail(authentication);
        List<PublicPostResponse> posts = postPortalUseCase.getMyPosts(email);
        return ResponseEntity.ok(ApiResponse.success(posts, "Get my posts successfully"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PublicPostResponse>> createPost(
            @RequestBody UpsertRecruitmentPostRequest request,
            Authentication authentication) {
        String email = resolveAuthenticatedEmail(authentication);
        PublicPostResponse response = postPortalUseCase.createPost(email, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Create post successfully"));
    }

    @PutMapping(ApiEndpoints.POST_BY_ID)
    public ResponseEntity<ApiResponse<PublicPostResponse>> updatePost(
            @PathVariable UUID postId,
            @RequestBody UpsertRecruitmentPostRequest request,
            Authentication authentication) {
        String email = resolveAuthenticatedEmail(authentication);
        PublicPostResponse response = postPortalUseCase.updatePost(email, postId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Update post successfully"));
    }

    @DeleteMapping(ApiEndpoints.POST_BY_ID)
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @PathVariable UUID postId,
            Authentication authentication) {
        String email = resolveAuthenticatedEmail(authentication);
        if (!postPortalUseCase.deletePost(email, postId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Post not found", "POST_NOT_FOUND"));
        }
        return ResponseEntity.ok(ApiResponse.success(null, "Delete post successfully"));
    }

    private String resolveAuthenticatedEmail(Authentication authentication) {
        String email = resolveOptionalAuthenticatedEmail(authentication);
        if (!StringUtils.hasText(email)) {
            throw new AuthException("Authentication required");
        }
        return email;
    }

    private String resolveOptionalAuthenticatedEmail(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        String email = String.valueOf(authentication.getPrincipal());
        if (!StringUtils.hasText(email)) {
            return null;
        }
        return email;
    }
}
