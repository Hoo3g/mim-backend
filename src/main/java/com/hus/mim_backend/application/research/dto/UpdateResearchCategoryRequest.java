package com.hus.mim_backend.application.research.dto;

/**
 * Request DTO for updating a research category.
 */
public class UpdateResearchCategoryRequest {
    private String name;
    private Integer sortOrder;
    private Boolean active;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
