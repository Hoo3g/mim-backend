package com.hus.mim_backend.domain.post.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * SavedPost entity - Bookmarked/saved posts
 * Maps to: saved_posts table
 */
public class SavedPost {
    private UUID userId;
    private UUID postId;
    private LocalDateTime createdAt;

    // TODO: Implement getters/setters
    // TODO: Implement constructor
}
