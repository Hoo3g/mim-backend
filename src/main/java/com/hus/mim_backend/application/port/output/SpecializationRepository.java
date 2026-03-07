package com.hus.mim_backend.application.port.output;

import com.hus.mim_backend.application.research.dto.ResearchCategoryResponse;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Output port for specialization taxonomy read/write operations.
 */
public interface SpecializationRepository {
    List<ResearchCategoryResponse> findActiveSpecializations();

    List<ResearchCategoryResponse> findAllSpecializations();

    Optional<ResearchCategoryResponse> findById(UUID specializationId);

    Optional<String> findActiveSpecializationName(String specializationName);

    boolean existsSpecializationWithSameName(String specializationName);

    boolean existsOtherSpecializationWithSameName(UUID specializationId, String specializationName);

    UUID createSpecialization(String specializationName, int sortOrder, boolean active);

    int updateSpecialization(UUID specializationId, String specializationName, int sortOrder, boolean active);

    int deactivateSpecialization(UUID specializationId);
}
