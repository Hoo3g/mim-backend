package com.hus.mim_backend.application.profile.service;

import com.hus.mim_backend.application.port.output.StudentRepository;
import com.hus.mim_backend.application.profile.dto.StudentProfileResponse;
import com.hus.mim_backend.application.profile.dto.UpdateStudentProfileRequest;
import com.hus.mim_backend.application.profile.usecase.ManageStudentProfileUseCase;
import com.hus.mim_backend.domain.profile.model.Student;
import com.hus.mim_backend.domain.shared.DomainException;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service for student profile management.
 * Implements only ManageStudentProfileUseCase — no method signature conflicts.
 */
public class StudentProfileService implements ManageStudentProfileUseCase {

    private final StudentRepository studentRepository;

    public StudentProfileService(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    @Override
    public StudentProfileResponse getProfile(UUID userId) {
        if (userId == null) {
            throw new DomainException("userId is required");
        }
        Student student = studentRepository.findById(userId)
                .orElseThrow(() -> new DomainException("Student profile not found"));
        return toResponse(student);
    }

    @Override
    public StudentProfileResponse updateProfile(UUID userId, UpdateStudentProfileRequest request) {
        if (userId == null) {
            throw new DomainException("userId is required");
        }
        UpdateStudentProfileRequest safeRequest = request == null ? new UpdateStudentProfileRequest() : request;

        Student student = studentRepository.findById(userId)
                .orElseGet(() -> Student.builder().id(userId).build());

        if (safeRequest.getFirstName() != null) {
            student.setFirstName(trimToNull(safeRequest.getFirstName()));
        }
        if (safeRequest.getLastName() != null) {
            student.setLastName(trimToNull(safeRequest.getLastName()));
        }
        if (safeRequest.getUniversity() != null) {
            student.setUniversity(trimToNull(safeRequest.getUniversity()));
        }
        if (safeRequest.getMajor() != null) {
            student.setMajor(trimToNull(safeRequest.getMajor()));
        }
        if (safeRequest.getBio() != null) {
            student.setBio(trimToNull(safeRequest.getBio()));
        }
        if (safeRequest.getCvUrl() != null) {
            student.setCvUrl(trimToNull(safeRequest.getCvUrl()));
        }
        if (safeRequest.getStudentType() != null) {
            student.setStudentType(trimToNull(safeRequest.getStudentType()));
        }
        student.setUpdatedAt(LocalDateTime.now());

        Student saved = studentRepository.save(student);
        return toResponse(saved);
    }

    private StudentProfileResponse toResponse(Student student) {
        return StudentProfileResponse.builder()
                .id(student.getId())
                .firstName(student.getFirstName())
                .lastName(student.getLastName())
                .university(student.getUniversity())
                .major(student.getMajor())
                .bio(student.getBio())
                .cvUrl(student.getCvUrl())
                .studentType(student.getStudentType())
                .build();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
