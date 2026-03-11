package com.hus.mim_backend.infrastructure.adapter.web.storage.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * Upload response for research PDF storage.
 */
@Setter
@Getter
public class ResearchPdfUploadResponse {
    private String objectKey;
    private String fileUrl;

    public ResearchPdfUploadResponse() {
    }

    public ResearchPdfUploadResponse(String objectKey, String fileUrl) {
        this.objectKey = objectKey;
        this.fileUrl = fileUrl;
    }

}

