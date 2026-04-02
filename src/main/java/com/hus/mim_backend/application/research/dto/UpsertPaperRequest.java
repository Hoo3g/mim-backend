package com.hus.mim_backend.application.research.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Request DTO for create/update research paper via portal editor.
 */
public class UpsertPaperRequest {
    private String title;
    private String abstractText;
    private String pdfUrl;
    private String researchArea;
    private String paperType;
    private Integer publicationYear;
    private String journalConference;
    private String category;
    private String authorName;
    private List<String> coAuthorStudentIds;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @JsonProperty("abstract")
    public String getAbstractText() {
        return abstractText;
    }

    @JsonProperty("abstract")
    public void setAbstractText(String abstractText) {
        this.abstractText = abstractText;
    }

    public String getPdfUrl() {
        return pdfUrl;
    }

    public void setPdfUrl(String pdfUrl) {
        this.pdfUrl = pdfUrl;
    }

    public String getResearchArea() {
        return researchArea;
    }

    public void setResearchArea(String researchArea) {
        this.researchArea = researchArea;
    }

    public String getPaperType() {
        return paperType;
    }

    public void setPaperType(String paperType) {
        this.paperType = paperType;
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public List<String> getCoAuthorStudentIds() {
        return coAuthorStudentIds;
    }

    public void setCoAuthorStudentIds(List<String> coAuthorStudentIds) {
        this.coAuthorStudentIds = coAuthorStudentIds;
    }
}
