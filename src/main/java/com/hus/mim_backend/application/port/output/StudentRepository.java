package com.hus.mim_backend.application.port.output;

import com.hus.mim_backend.domain.profile.model.Student;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Output port for Student profile persistence operations
 */
public interface StudentRepository {
    Optional<Student> findById(UUID id);

    List<Student> findByUniversity(String university);

    List<Student> findByMajor(String major);

    List<Student> findByStudentType(String studentType);

    Student save(Student student);

    void deleteById(UUID id);
}
