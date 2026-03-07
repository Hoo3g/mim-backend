package com.hus.mim_backend.application.port.output;

import com.hus.mim_backend.application.post.dto.PublicPostResponse;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Output port for public recruitment posts.
 */
public interface PublicPostRepository {
    List<PublicPostResponse> findAllApprovedPosts();

    Optional<PublicPostResponse> findApprovedPostById(UUID postId);
}
