package com.hus.mim_backend.application.port.output;

import com.hus.mim_backend.domain.profile.model.Company;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Output port for Company profile persistence operations
 */
public interface CompanyRepository {
    Optional<Company> findById(UUID id);

    List<Company> findByIndustry(String industry);

    List<Company> findByLocation(String location);

    Company save(Company company);

    void deleteById(UUID id);
}
