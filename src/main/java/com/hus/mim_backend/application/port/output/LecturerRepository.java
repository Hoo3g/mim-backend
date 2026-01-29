package com.hus.mim_backend.application.port.output;

import com.hus.mim_backend.domain.profile.model.Lecturer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Output port for Lecturer profile persistence operations
 */
public interface LecturerRepository {
    Optional<Lecturer> findById(UUID id);

    List<Lecturer> findByTitle(String title);

    List<Lecturer> findByAcademicRank(String academicRank);

    Lecturer save(Lecturer lecturer);

    void deleteById(UUID id);
}
