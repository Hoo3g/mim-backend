package com.hus.mim_backend.application.research.dto;

import java.util.List;
import java.util.UUID;

public class CreatePaperRequest {
    private String title;
    private String abstractText;
    private String pdfUrl;
    private Integer publicationYear;
    private List<UUID> studentAuthorIds;
    private List<UUID> lecturerAuthorIds;

    // TODO: Implement getters/setters
}
