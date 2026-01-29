package com.hus.mim_backend.application.profile.service;

import com.hus.mim_backend.application.profile.dto.*;
import com.hus.mim_backend.application.profile.usecase.*;
import com.hus.mim_backend.application.port.output.*;
import java.util.UUID;

/**
 * Service orchestrating Profile management use cases
 */
public class ProfileServiceImpl implements
        ManageStudentProfileUseCase,
        ManageCompanyProfileUseCase,
        ManageLecturerProfileUseCase {

    private final StudentRepository studentRepository;
    private final CompanyRepository companyRepository;
    private final LecturerRepository lecturerRepository;

    public ProfileServiceImpl(StudentRepository studentRepository,
            CompanyRepository companyRepository,
            LecturerRepository lecturerRepository) {
        this.studentRepository = studentRepository;
        this.companyRepository = companyRepository;
        this.lecturerRepository = lecturerRepository;
    }

    @Override
    public StudentProfileResponse getProfile(UUID userId) {
        // TODO: Get student from repository and map to response
        return null;
    }

    @Override
    public StudentProfileResponse updateProfile(UUID userId, UpdateStudentProfileRequest request) {
        // TODO: Update student profile fields and save
        return null;
    }

    @Override
    public CompanyProfileResponse getProfile(UUID userId) {
        // TODO: Get company from repository and map to response
        // Note: Java allows overloading by parameter, but here we have conflicts if we
        // don't watch out.
        // In reality, these would likely be separate services or specifically named
        // methods.
        return null;
    }

    @Override
    public CompanyProfileResponse updateProfile(UUID userId, UpdateCompanyProfileRequest request) {
        // TODO: Update company profile
        return null;
    }

    @Override
    public LecturerProfileResponse updateProfile(UUID userId, UpdateLecturerProfileRequest request) {
        // TODO: Update lecturer profile
        return null;
    }
}
