package com.hus.mim_backend.application.profile.service;

import com.hus.mim_backend.application.port.output.LecturerRepository;
import com.hus.mim_backend.application.profile.dto.LecturerProfileResponse;
import com.hus.mim_backend.application.profile.dto.UpdateLecturerProfileRequest;
import com.hus.mim_backend.application.profile.usecase.ManageLecturerProfileUseCase;
import com.hus.mim_backend.domain.profile.model.Lecturer;
import com.hus.mim_backend.domain.shared.DomainException;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
        if (userId == null) {
            throw new DomainException("userId is required");
        }
        Lecturer lecturer = lecturerRepository.findById(userId)
                .orElseThrow(() -> new DomainException("Lecturer profile not found"));
        return toResponse(lecturer);
    }

    @Override
    public LecturerProfileResponse updateProfile(UUID userId, UpdateLecturerProfileRequest request) {
        if (userId == null) {
            throw new DomainException("userId is required");
        }
        UpdateLecturerProfileRequest safeRequest = request == null ? new UpdateLecturerProfileRequest() : request;

        Lecturer lecturer = lecturerRepository.findById(userId)
                .orElseGet(() -> Lecturer.builder().id(userId).build());

        if (safeRequest.getFirstName() != null) {
            lecturer.setFirstName(trimToNull(safeRequest.getFirstName()));
        }
        if (safeRequest.getLastName() != null) {
            lecturer.setLastName(trimToNull(safeRequest.getLastName()));
        }
        if (safeRequest.getTitle() != null) {
            lecturer.setTitle(trimToNull(safeRequest.getTitle()));
        }
        if (safeRequest.getAcademicRank() != null) {
            lecturer.setAcademicRank(trimToNull(safeRequest.getAcademicRank()));
        }
        if (safeRequest.getBio() != null) {
            lecturer.setBio(trimToNull(safeRequest.getBio()));
        }
        if (safeRequest.getResearchInterests() != null) {
            lecturer.setResearchInterests(new ArrayList<>(safeRequest.getResearchInterests()));
        }
        lecturer.setUpdatedAt(LocalDateTime.now());

        Lecturer saved = lecturerRepository.save(lecturer);
        return toResponse(saved);
    }

    private LecturerProfileResponse toResponse(Lecturer lecturer) {
        LecturerProfileResponse response = new LecturerProfileResponse();
        response.setId(lecturer.getId());
        response.setFirstName(lecturer.getFirstName());
        response.setLastName(lecturer.getLastName());
        response.setTitle(lecturer.getTitle());
        response.setAcademicRank(lecturer.getAcademicRank());
        response.setBio(lecturer.getBio());
        response.setResearchInterests(lecturer.getResearchInterests() == null
                ? java.util.List.of()
                : new ArrayList<>(lecturer.getResearchInterests()));
        return response;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
