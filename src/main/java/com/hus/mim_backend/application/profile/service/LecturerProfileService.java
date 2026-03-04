package com.hus.mim_backend.application.profile.service;

import com.hus.mim_backend.application.port.output.LecturerRepository;
import com.hus.mim_backend.application.profile.dto.LecturerProfileResponse;
import com.hus.mim_backend.application.profile.dto.UpdateLecturerProfileRequest;
import com.hus.mim_backend.application.profile.usecase.ManageLecturerProfileUseCase;

import java.util.UUID;

/**
 * Service for lecturer profile management.
 */
public class LecturerProfileService implements ManageLecturerProfileUseCase {

    private final LecturerRepository lecturerRepository;

    public LecturerProfileService(LecturerRepository lecturerRepository) {
        this.lecturerRepository = lecturerRepository;
    }

    @Override
    public LecturerProfileResponse getProfile(UUID userId) {
        // TODO: fetch lecturer and map to response
        return null;
    }

    @Override
    public LecturerProfileResponse updateProfile(UUID userId, UpdateLecturerProfileRequest request) {
        // TODO: update lecturer fields and persist
        return null;
    }
}
