package com.hus.mim_backend.domain.research.model;

import com.hus.mim_backend.domain.shared.DomainException;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * ResearchPaper aggregate - Academic research papers
 * Maps to: research_papers table
 */
@Getter
@Setter
public class ResearchPaper {
    private UUID id;
    private String title;
    private String abstractText; // Named 'abstract' in SQL, reserved word in Java
    private String pdfUrl;
    private Integer publicationYear;
    private String journalConference;
    private String researchArea;
    private PaperCategory category; // STUDENT, LECTURER
    private List<PaperAuthor> authors;
    private int viewCount;
    private int downloadCount;
    private int citationCount;
    private ApprovalStatus approvalStatus;
    private UUID moderatorId;
    private String moderationComment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ResearchPaper() {
    }

    public static ResearchPaperBuilder builder() {
        return new ResearchPaperBuilder();
    }

    public void incrementView() {
        this.viewCount++;
        this.updatedAt = LocalDateTime.now();
    }

    public void incrementDownload() {
        this.downloadCount++;
        this.updatedAt = LocalDateTime.now();
    }

    public void approve(UUID moderatorId, String comment) {
        this.approvalStatus = ApprovalStatus.APPROVED;
        this.moderatorId = moderatorId;
        this.moderationComment = comment;
        this.updatedAt = LocalDateTime.now();
    }

    public void reject(UUID moderatorId, String comment) {
        if (comment == null || comment.isBlank()) {
            throw new DomainException("Rejection comment is required");
        }
        this.approvalStatus = ApprovalStatus.REJECTED;
        this.moderatorId = moderatorId;
        this.moderationComment = comment;
        this.updatedAt = LocalDateTime.now();
    }

    public enum PaperCategory {
        STUDENT,
        LECTURER
    }

    public enum ApprovalStatus {
        PENDING,
        APPROVED,
        REJECTED
    }

    public static class ResearchPaperBuilder {
        private final ResearchPaper paper = new ResearchPaper();

        public ResearchPaperBuilder id(UUID id) {
            paper.id = id;
            return this;
        }

        public ResearchPaperBuilder title(String title) {
            paper.title = title;
            return this;
        }

        public ResearchPaperBuilder abstractText(String abstractText) {
            paper.abstractText = abstractText;
            return this;
        }

        public ResearchPaperBuilder pdfUrl(String pdfUrl) {
            paper.pdfUrl = pdfUrl;
            return this;
        }

        public ResearchPaperBuilder publicationYear(Integer publicationYear) {
            paper.publicationYear = publicationYear;
            return this;
        }

        public ResearchPaperBuilder journalConference(String journalConference) {
            paper.journalConference = journalConference;
            return this;
        }

        public ResearchPaperBuilder researchArea(String researchArea) {
            paper.researchArea = researchArea;
            return this;
        }

        public ResearchPaperBuilder category(PaperCategory category) {
            paper.category = category;
            return this;
        }

        public ResearchPaperBuilder authors(List<PaperAuthor> authors) {
            paper.authors = authors;
            return this;
        }

        public ResearchPaperBuilder viewCount(int viewCount) {
            paper.viewCount = viewCount;
            return this;
        }

        public ResearchPaperBuilder downloadCount(int downloadCount) {
            paper.downloadCount = downloadCount;
            return this;
        }

        public ResearchPaperBuilder citationCount(int citationCount) {
            paper.citationCount = citationCount;
            return this;
        }

        public ResearchPaperBuilder approvalStatus(ApprovalStatus approvalStatus) {
            paper.approvalStatus = approvalStatus;
            return this;
        }

        public ResearchPaperBuilder moderatorId(UUID moderatorId) {
            paper.moderatorId = moderatorId;
            return this;
        }

        public ResearchPaperBuilder moderationComment(String moderationComment) {
            paper.moderationComment = moderationComment;
            return this;
        }

        public ResearchPaperBuilder createdAt(LocalDateTime createdAt) {
            paper.createdAt = createdAt;
            return this;
        }

        public ResearchPaperBuilder updatedAt(LocalDateTime updatedAt) {
            paper.updatedAt = updatedAt;
            return this;
        }

        public ResearchPaper build() {
            return paper;
        }
    }
}
