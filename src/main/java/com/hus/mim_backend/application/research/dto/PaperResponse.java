package com.hus.mim_backend.application.research.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class PaperResponse {

    private UUID id;
    private String title;
    private String paperAbstract;
    private String pdfUrl;
    private Integer publicationYear;
    private String journalConference;
    private String researchArea;
    private String category;
    private String paperType;
    private int viewCount;
    private int downloadCount;
    private int bookmarkCount;
    private String approvalStatus;
    private String moderationComment;
    private List<PaperAuthorResponse> authors = new ArrayList<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

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

    public String getPaperType() {
        return paperType;
    }

    public void setPaperType(String paperType) {
        this.paperType = paperType;
    }

    public int getViewCount() {
        return viewCount;
    }

    public void setViewCount(int viewCount) {
        this.viewCount = viewCount;
    }

    public int getDownloadCount() {
        return downloadCount;
    }

    public void setDownloadCount(int downloadCount) {
        this.downloadCount = downloadCount;
    }

    public int getBookmarkCount() {
        return bookmarkCount;
    }

    public void setBookmarkCount(int bookmarkCount) {
        this.bookmarkCount = bookmarkCount;
    }

    public String getApprovalStatus() {
        return approvalStatus;
    }

    public void setApprovalStatus(String approvalStatus) {
        this.approvalStatus = approvalStatus;
    }

    public String getModerationComment() {
        return moderationComment;
    }

    public void setModerationComment(String moderationComment) {
        this.moderationComment = moderationComment;
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

    @Setter
    @Getter
    public static class PaperAuthorResponse {
        private String studentId;
        private String name;
        private String authorType;
        private boolean isMainAuthor;
        private int authorOrder;
        private boolean canViewProfile = true;

    }
}
