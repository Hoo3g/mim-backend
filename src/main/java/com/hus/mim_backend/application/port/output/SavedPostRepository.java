package com.hus.mim_backend.application.port.output;

import com.hus.mim_backend.domain.post.model.SavedPost;

import java.util.List;
import java.util.UUID;

/**
 * Output port for SavedPost (bookmarks) persistence operations
 */
public interface SavedPostRepository {
    List<SavedPost> findByUserId(UUID userId);

    boolean existsByUserIdAndPostId(UUID userId, UUID postId);

    void save(UUID userId, UUID postId);

    void deleteByUserIdAndPostId(UUID userId, UUID postId);
}
