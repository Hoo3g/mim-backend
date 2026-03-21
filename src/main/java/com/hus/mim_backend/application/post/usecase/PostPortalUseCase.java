package com.hus.mim_backend.application.post.usecase;

import com.hus.mim_backend.application.post.dto.PublicPostResponse;
import com.hus.mim_backend.application.post.dto.UpsertRecruitmentPostRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Input port for recruitment post portal APIs used by authenticated users.
 */
public interface PostPortalUseCase {
    List<PublicPostResponse> getMyPosts(String email);

    Optional<PublicPostResponse> getMyPostById(String email, UUID postId);

    Optional<PublicPostResponse> getPostByIdForViewer(UUID postId, String viewerEmail);

    PublicPostResponse createPost(String email, UpsertRecruitmentPostRequest request);

    PublicPostResponse updatePost(String email, UUID postId, UpsertRecruitmentPostRequest request);

    boolean deletePost(String email, UUID postId);
}
