package com.hus.mim_backend.infrastructure.adapter.persistence.research;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class SavedResearchPaperId implements Serializable {
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "paper_id")
    private UUID paperId;

    public SavedResearchPaperId() {
    }

    public SavedResearchPaperId(UUID userId, UUID paperId) {
        this.userId = userId;
        this.paperId = paperId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getPaperId() {
        return paperId;
    }

    public void setPaperId(UUID paperId) {
        this.paperId = paperId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SavedResearchPaperId that)) {
            return false;
        }
        return Objects.equals(userId, that.userId) && Objects.equals(paperId, that.paperId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, paperId);
    }
}
