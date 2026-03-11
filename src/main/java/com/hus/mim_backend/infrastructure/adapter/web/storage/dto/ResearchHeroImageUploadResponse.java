package com.hus.mim_backend.infrastructure.adapter.web.storage.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * Upload response for research hero image storage.
 */
@Setter
@Getter
public class ResearchHeroImageUploadResponse {
    private String objectKey;
    private String fileUrl;

    public ResearchHeroImageUploadResponse() {
    }

    public ResearchHeroImageUploadResponse(String objectKey, String fileUrl) {
        this.objectKey = objectKey;
        this.fileUrl = fileUrl;
    }

}
