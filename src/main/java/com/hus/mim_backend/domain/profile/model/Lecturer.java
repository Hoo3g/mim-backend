package com.hus.mim_backend.domain.profile.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Lecturer aggregate - Lecturer/Faculty profile linked to User
 * Maps to: lecturers table
 */
public class Lecturer {
    private UUID id; // Same as user_id (1:1 relationship)
    private String firstName;
    private String lastName;
    private String title; // PGS.TS, GS.TS, TS
    private String academicRank;
    private String bio;
    private List<String> researchInterests;
    private LocalDateTime updatedAt;

    // TODO: Implement getters/setters
    // TODO: Implement builder pattern
    // TODO: Implement getFullName() domain logic
    // TODO: Implement getDisplayTitle() combining title + name
}
