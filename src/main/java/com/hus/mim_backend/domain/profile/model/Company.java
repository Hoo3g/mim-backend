package com.hus.mim_backend.domain.profile.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Company aggregate - Company profile linked to User
 * Maps to: companies table
 */
public class Company {
    private UUID id; // Same as user_id (1:1 relationship)
    private String name;
    private String industry;
    private String website;
    private String location;
    private String description;
    private LocalDateTime updatedAt;

    // TODO: Implement getters/setters
    // TODO: Implement builder pattern
}
