package com.hus.mim_backend.infrastructure.adapter.web.storage.dto;

/**
 * Upload response for research PDF storage.
 */
public class ResearchPdfUploadResponse {
    private String objectKey;
    private String fileUrl;

    public ResearchPdfUploadResponse() {
    }

    public ResearchPdfUploadResponse(String objectKey, String fileUrl) {
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

