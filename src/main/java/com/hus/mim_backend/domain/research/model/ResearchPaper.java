package com.hus.mim_backend.domain.research.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * ResearchPaper aggregate - Academic research papers
 * Maps to: research_papers table
 */
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

    // TODO: Implement getters/setters
    // TODO: Implement builder pattern
    // TODO: Implement incrementView() domain logic
    // TODO: Implement incrementDownload() domain logic
    // TODO: Implement approve(moderatorId, comment) domain logic
    // TODO: Implement reject(moderatorId, comment) domain logic

    public enum PaperCategory {
        STUDENT,
        LECTURER
    }

    public enum ApprovalStatus {
        PENDING,
        APPROVED,
        REJECTED
    }
}
