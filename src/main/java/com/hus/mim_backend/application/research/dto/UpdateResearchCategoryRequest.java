package com.hus.mim_backend.application.research.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * Request DTO for updating a research category.
 */
@Setter
@Getter
public class UpdateResearchCategoryRequest {
    private String name;
    private Integer sortOrder;
    private Boolean active;

}
