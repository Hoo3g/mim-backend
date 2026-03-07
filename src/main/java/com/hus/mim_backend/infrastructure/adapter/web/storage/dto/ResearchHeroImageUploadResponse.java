package com.hus.mim_backend.infrastructure.adapter.web.storage.dto;

/**
 * Upload response for research hero image storage.
 */
public class ResearchHeroImageUploadResponse {
    private String objectKey;
    private String fileUrl;

    public ResearchHeroImageUploadResponse() {
    }

    public ResearchHeroImageUploadResponse(String objectKey, String fileUrl) {
        this.objectKey = objectKey;
        this.fileUrl = fileUrl;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public void setObjectKey(String objectKey) {
        this.objectKey = objectKey;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }
}
