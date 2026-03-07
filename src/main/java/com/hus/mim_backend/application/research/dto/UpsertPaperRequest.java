package com.hus.mim_backend.application.research.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request DTO for create/update research paper via portal editor.
 */
public class UpsertPaperRequest {
    private String title;
    private String abstractText;
    private String pdfUrl;
    private String researchArea;

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
}
