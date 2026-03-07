package com.hus.mim_backend.application.post.usecase;

import com.hus.mim_backend.application.post.dto.PublicPostResponse;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Input port for public post read APIs.
 */
public interface QueryPublicPostsUseCase {
    List<PublicPostResponse> getPosts();

    Optional<PublicPostResponse> getPostById(UUID postId);
}
