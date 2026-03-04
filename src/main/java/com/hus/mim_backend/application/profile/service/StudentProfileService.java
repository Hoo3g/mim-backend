package com.hus.mim_backend.application.profile.service;

import com.hus.mim_backend.application.port.output.StudentRepository;
import com.hus.mim_backend.application.profile.dto.StudentProfileResponse;
import com.hus.mim_backend.application.profile.dto.UpdateStudentProfileRequest;
import com.hus.mim_backend.application.profile.usecase.ManageStudentProfileUseCase;

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
        // TODO: fetch student and map to response
        return null;
    }

    @Override
    public StudentProfileResponse updateProfile(UUID userId, UpdateStudentProfileRequest request) {
        // TODO: update student fields and persist
        return null;
    }
}
