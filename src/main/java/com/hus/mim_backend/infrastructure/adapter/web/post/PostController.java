package com.hus.mim_backend.infrastructure.adapter.web.post;

import com.hus.mim_backend.application.post.dto.PublicPostResponse;
import com.hus.mim_backend.application.post.usecase.QueryPublicPostsUseCase;
import com.hus.mim_backend.shared.api.ApiResponse;
import com.hus.mim_backend.shared.constants.ApiEndpoints;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
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

    public PostController(QueryPublicPostsUseCase queryPublicPostsUseCase) {
        this.queryPublicPostsUseCase = queryPublicPostsUseCase;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PublicPostResponse>>> getPosts() {
        List<PublicPostResponse> posts = queryPublicPostsUseCase.getPosts();
        return ResponseEntity.ok(ApiResponse.success(posts, "Get posts successfully"));
    }

    @GetMapping(ApiEndpoints.POST_BY_ID)
    public ResponseEntity<ApiResponse<PublicPostResponse>> getPostById(@PathVariable UUID postId) {
        return queryPublicPostsUseCase.getPostById(postId)
                .map(post -> ResponseEntity.ok(ApiResponse.success(post, "Get post successfully")))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Post not found", "POST_NOT_FOUND")));
    }
}
