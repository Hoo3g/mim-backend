package com.hus.mim_backend.domain.profile.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Student aggregate - Student profile linked to User
 * Maps to: students table
 */
public class Student {
    private UUID id; // Same as user_id (1:1 relationship)
    private String firstName;
    private String lastName;
    private String university;
    private String major;
    private String bio;
    private String cvUrl; // Default CV URL
    private String studentType; // PUPIL, UNIVERSITY_STUDENT
    private LocalDateTime updatedAt;

    // TODO: Implement getters/setters
    // TODO: Implement builder pattern
    // TODO: Implement getFullName() domain logic
    // TODO: Implement validation for studentType
}
