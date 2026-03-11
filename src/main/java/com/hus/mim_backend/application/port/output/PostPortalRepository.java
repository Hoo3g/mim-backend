package com.hus.mim_backend.application.port.output;

import com.hus.mim_backend.application.post.dto.PublicPostResponse;
import com.hus.mim_backend.application.post.dto.UpsertRecruitmentPostRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Output port for recruitment post portal persistence operations.
 */
public interface PostPortalRepository {
    Optional<UUID> findUserIdByEmail(String email);

    Optional<String> findPrimaryRole(UUID userId);

    Optional<PublicPostResponse> findPostByIdForViewer(UUID postId, UUID viewerId);

    Optional<PublicPostResponse> findPostByIdForAuthor(UUID postId, UUID authorId);

    List<PublicPostResponse> findPostsByAuthor(UUID authorId);

    UUID createPost(UUID authorId, UpsertRecruitmentPostRequest request, String displayInfoJson, String tagsCsv);

    boolean updatePostByAuthor(UUID postId,
                               UUID authorId,
                               UpsertRecruitmentPostRequest request,
                               String displayInfoJson,
                               String tagsCsv);

    void replaceLinkedResearchPapers(UUID postId, List<UUID> paperIds);
}
