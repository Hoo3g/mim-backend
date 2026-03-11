package com.hus.mim_backend.domain.profile.model;

import com.hus.mim_backend.domain.shared.DomainException;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Student aggregate - Student profile linked to User
 * Maps to: students table
 */
@Getter
@Setter
public class Student {
    private static final Set<String> ALLOWED_STUDENT_TYPES = Set.of("PUPIL", "UNIVERSITY_STUDENT");

    private UUID id; // Same as user_id (1:1 relationship)
    private String firstName;
    private String lastName;
    private String university;
    private String major;
    private String bio;
    private String cvUrl; // Default CV URL
    private String studentType; // PUPIL, UNIVERSITY_STUDENT
    private LocalDateTime updatedAt;

    public Student() {
    }

    public static StudentBuilder builder() {
        return new StudentBuilder();
    }

    public String getFullName() {
        String first = firstName == null ? "" : firstName.trim();
        String last = lastName == null ? "" : lastName.trim();
        String fullName = (first + " " + last).trim();
        return fullName.isEmpty() ? null : fullName;
    }

    public void setStudentType(String studentType) {
        if (studentType == null || studentType.isBlank()) {
            this.studentType = null;
            return;
        }
        String normalized = studentType.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_STUDENT_TYPES.contains(normalized)) {
            throw new DomainException("studentType must be PUPIL or UNIVERSITY_STUDENT");
        }
        this.studentType = normalized;
    }

    public static class StudentBuilder {
        private final Student student = new Student();

        public StudentBuilder id(UUID id) {
            student.id = id;
            return this;
        }

        public StudentBuilder firstName(String firstName) {
            student.firstName = firstName;
            return this;
        }

        public StudentBuilder lastName(String lastName) {
            student.lastName = lastName;
            return this;
        }

        public StudentBuilder university(String university) {
            student.university = university;
            return this;
        }

        public StudentBuilder major(String major) {
            student.major = major;
            return this;
        }

        public StudentBuilder bio(String bio) {
            student.bio = bio;
            return this;
        }

        public StudentBuilder cvUrl(String cvUrl) {
            student.cvUrl = cvUrl;
            return this;
        }

        public StudentBuilder studentType(String studentType) {
            student.setStudentType(studentType);
            return this;
        }

        public StudentBuilder updatedAt(LocalDateTime updatedAt) {
            student.updatedAt = updatedAt;
            return this;
        }

        public Student build() {
            return student;
        }
    }
}
