package com.hus.mim_backend.application.research.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PaperResponse {
    private UUID id;
    private String title;
    private String paperAbstract;
    private String pdfUrl;
    private Integer publicationYear;
    private String journalConference;
    private String researchArea;
    private String category;
    private int viewCount;
    private List<PaperAuthorResponse> authors = new ArrayList<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAbstract() {
        return paperAbstract;
    }

    public void setAbstract(String paperAbstract) {
        this.paperAbstract = paperAbstract;
    }

    public String getPdfUrl() {
        return pdfUrl;
    }

    public void setPdfUrl(String pdfUrl) {
        this.pdfUrl = pdfUrl;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getViewCount() {
        return viewCount;
    }

    public void setViewCount(int viewCount) {
        this.viewCount = viewCount;
    }

    public Integer getPublicationYear() {
        return publicationYear;
    }

    public void setPublicationYear(Integer publicationYear) {
        this.publicationYear = publicationYear;
    }

    public String getJournalConference() {
        return journalConference;
    }

    public void setJournalConference(String journalConference) {
        this.journalConference = journalConference;
    }

    public String getResearchArea() {
        return researchArea;
    }

    public void setResearchArea(String researchArea) {
        this.researchArea = researchArea;
    }

    public List<PaperAuthorResponse> getAuthors() {
        return authors;
    }

    public void setAuthors(List<PaperAuthorResponse> authors) {
        this.authors = authors;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public static class PaperAuthorResponse {
        private String studentId;
        private String name;
        private boolean isMainAuthor;
        private int authorOrder;

        public String getStudentId() {
            return studentId;
        }

        public void setStudentId(String studentId) {
            this.studentId = studentId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public boolean isMainAuthor() {
            return isMainAuthor;
        }

        public void setMainAuthor(boolean mainAuthor) {
            isMainAuthor = mainAuthor;
        }

        public int getAuthorOrder() {
            return authorOrder;
        }

        public void setAuthorOrder(int authorOrder) {
            this.authorOrder = authorOrder;
        }
    }
}
