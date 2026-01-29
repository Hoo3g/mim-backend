package com.hus.mim_backend.application.port.output;

import com.hus.mim_backend.domain.post.model.Post;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Output port for Post persistence operations
 */
public interface PostRepository {
    Optional<Post> findById(UUID id);

    List<Post> findByAuthorId(UUID authorId);

    List<Post> findByStatus(String status);

    List<Post> findByApprovalStatus(String approvalStatus);

    List<Post> findByPostType(String postType);

    List<Post> findByJobType(String jobType);

    List<Post> searchByTitleOrDescription(String keyword);

    List<Post> findByTags(List<String> tags);

    Post save(Post post);

    void deleteById(UUID id);
}
