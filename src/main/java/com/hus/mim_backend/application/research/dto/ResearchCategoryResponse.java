package com.hus.mim_backend.application.research.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for research category taxonomy item.
 */
@Setter
@Getter
public class ResearchCategoryResponse {
    private UUID id;
    private String name;
    private Integer sortOrder;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
