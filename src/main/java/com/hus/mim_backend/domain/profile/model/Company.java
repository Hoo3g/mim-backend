package com.hus.mim_backend.domain.profile.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Company aggregate - Company profile linked to User
 * Maps to: companies table
 */
@Getter
@Setter
public class Company {
    private UUID id; // Same as user_id (1:1 relationship)
    private String name;
    private String industry;
    private String website;
    private String location;
    private String description;
    private LocalDateTime updatedAt;

    public Company() {
    }

    public static CompanyBuilder builder() {
        return new CompanyBuilder();
    }

    public static class CompanyBuilder {
        private final Company company = new Company();

        public CompanyBuilder id(UUID id) {
            company.id = id;
            return this;
        }

        public CompanyBuilder name(String name) {
            company.name = name;
            return this;
        }

        public CompanyBuilder industry(String industry) {
            company.industry = industry;
            return this;
        }

        public CompanyBuilder website(String website) {
            company.website = website;
            return this;
        }

        public CompanyBuilder location(String location) {
            company.location = location;
            return this;
        }

        public CompanyBuilder description(String description) {
            company.description = description;
            return this;
        }

        public CompanyBuilder updatedAt(LocalDateTime updatedAt) {
            company.updatedAt = updatedAt;
            return this;
        }

        public Company build() {
            return company;
        }
    }
}
