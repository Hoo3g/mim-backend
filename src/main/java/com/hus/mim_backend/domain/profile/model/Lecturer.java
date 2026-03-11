package com.hus.mim_backend.domain.profile.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Lecturer aggregate - Lecturer/Faculty profile linked to User
 * Maps to: lecturers table
 */
@Getter
@Setter
public class Lecturer {
    private UUID id; // Same as user_id (1:1 relationship)
    private String firstName;
    private String lastName;
    private String title; // PGS.TS, GS.TS, TS
    private String academicRank;
    private String bio;
    private List<String> researchInterests;
    private LocalDateTime updatedAt;

    public Lecturer() {
    }

    public static LecturerBuilder builder() {
        return new LecturerBuilder();
    }

    public String getFullName() {
        String first = firstName == null ? "" : firstName.trim();
        String last = lastName == null ? "" : lastName.trim();
        String fullName = (first + " " + last).trim();
        return fullName.isEmpty() ? null : fullName;
    }

    public String getDisplayTitle() {
        String name = getFullName();
        String normalizedTitle = title == null ? "" : title.trim();
        if (normalizedTitle.isEmpty()) {
            return name;
        }
        if (name == null || name.isBlank()) {
            return normalizedTitle;
        }
        return normalizedTitle + " " + name;
    }

    public static class LecturerBuilder {
        private final Lecturer lecturer = new Lecturer();

        public LecturerBuilder id(UUID id) {
            lecturer.id = id;
            return this;
        }

        public LecturerBuilder firstName(String firstName) {
            lecturer.firstName = firstName;
            return this;
        }

        public LecturerBuilder lastName(String lastName) {
            lecturer.lastName = lastName;
            return this;
        }

        public LecturerBuilder title(String title) {
            lecturer.title = title;
            return this;
        }

        public LecturerBuilder academicRank(String academicRank) {
            lecturer.academicRank = academicRank;
            return this;
        }

        public LecturerBuilder bio(String bio) {
            lecturer.bio = bio;
            return this;
        }

        public LecturerBuilder researchInterests(List<String> researchInterests) {
            lecturer.researchInterests = researchInterests;
            return this;
        }

        public LecturerBuilder updatedAt(LocalDateTime updatedAt) {
            lecturer.updatedAt = updatedAt;
            return this;
        }

        public Lecturer build() {
            return lecturer;
        }
    }
}
