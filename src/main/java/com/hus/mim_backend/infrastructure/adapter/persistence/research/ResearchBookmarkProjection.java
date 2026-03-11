package com.hus.mim_backend.infrastructure.adapter.persistence.research;

import java.time.LocalDateTime;
import java.util.UUID;

public interface ResearchBookmarkProjection {
    UUID getPaperId();

    String getTitle();

    String getResearchArea();

    String getCategory();

    Integer getPublicationYear();

    LocalDateTime getSavedAt();
}
