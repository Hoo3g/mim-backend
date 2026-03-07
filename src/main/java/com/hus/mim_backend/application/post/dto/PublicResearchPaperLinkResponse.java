package com.hus.mim_backend.application.post.dto;

import java.util.UUID;

/**
 * Linked approved research paper item for post response.
 */
public class PublicResearchPaperLinkResponse {
    private UUID id;
    private String title;
    private String url;

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

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
