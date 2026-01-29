package com.hus.mim_backend.domain.research.model;

import java.util.UUID;

/**
 * PaperAuthor entity - Many-to-many relationship between papers and authors
 * Maps to: paper_authors table
 */
public class PaperAuthor {
    private UUID id;
    private UUID paperId;
    private UUID studentId; // Nullable - can be student
    private UUID lecturerId; // Nullable - can be lecturer
    private boolean isMainAuthor;
    private int authorOrder;

    // TODO: Implement getters/setters
    // TODO: Implement builder pattern
    // TODO: Implement getAuthorId() - returns whichever is not null
    // TODO: Implement getAuthorType() - returns STUDENT or LECTURER
}
