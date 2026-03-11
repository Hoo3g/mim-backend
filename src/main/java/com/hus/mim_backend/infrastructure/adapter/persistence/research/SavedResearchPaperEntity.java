package com.hus.mim_backend.infrastructure.adapter.persistence.research;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "saved_research_papers")
public class SavedResearchPaperEntity {
    @EmbeddedId
    private SavedResearchPaperId id;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public SavedResearchPaperId getId() {
        return id;
    }

    public void setId(SavedResearchPaperId id) {
        this.id = id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
