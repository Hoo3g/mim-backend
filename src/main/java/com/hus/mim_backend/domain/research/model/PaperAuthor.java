package com.hus.mim_backend.domain.research.model;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * PaperAuthor entity - Many-to-many relationship between papers and authors
 * Maps to: paper_authors table
 */
@Getter
@Setter
public class PaperAuthor {
    private UUID id;
    private UUID paperId;
    private UUID studentId; // Nullable - can be student
    private UUID lecturerId; // Nullable - can be lecturer
    private boolean isMainAuthor;
    private int authorOrder;

    public PaperAuthor() {
    }

    public static PaperAuthorBuilder builder() {
        return new PaperAuthorBuilder();
    }

    public UUID getAuthorId() {
        return studentId != null ? studentId : lecturerId;
    }

    public AuthorType getAuthorType() {
        return studentId != null ? AuthorType.STUDENT : AuthorType.LECTURER;
    }

    public enum AuthorType {
        STUDENT,
        LECTURER
    }

    public static class PaperAuthorBuilder {
        private final PaperAuthor author = new PaperAuthor();

        public PaperAuthorBuilder id(UUID id) {
            author.id = id;
            return this;
        }

        public PaperAuthorBuilder paperId(UUID paperId) {
            author.paperId = paperId;
            return this;
        }

        public PaperAuthorBuilder studentId(UUID studentId) {
            author.studentId = studentId;
            return this;
        }

        public PaperAuthorBuilder lecturerId(UUID lecturerId) {
            author.lecturerId = lecturerId;
            return this;
        }

        public PaperAuthorBuilder isMainAuthor(boolean isMainAuthor) {
            author.isMainAuthor = isMainAuthor;
            return this;
        }

        public PaperAuthorBuilder authorOrder(int authorOrder) {
            author.authorOrder = authorOrder;
            return this;
        }

        public PaperAuthor build() {
            return author;
        }
    }
}
