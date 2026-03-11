package com.hus.mim_backend.domain.post.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * SavedPost entity - Bookmarked/saved posts
 * Maps to: saved_posts table
 */
@Getter
@Setter
public class SavedPost {
    private UUID userId;
    private UUID postId;
    private LocalDateTime createdAt;

    public SavedPost() {
    }

    public SavedPost(UUID userId, UUID postId) {
        this(userId, postId, LocalDateTime.now());
    }

    public SavedPost(UUID userId, UUID postId, LocalDateTime createdAt) {
        this.userId = userId;
        this.postId = postId;
        this.createdAt = createdAt;
    }
}
