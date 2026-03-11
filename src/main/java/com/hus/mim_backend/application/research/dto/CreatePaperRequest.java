package com.hus.mim_backend.application.research.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class CreatePaperRequest {
    private String title;
    private String abstractText;
    private String pdfUrl;
    private Integer publicationYear;
    private List<UUID> studentAuthorIds;
    private List<UUID> lecturerAuthorIds;
}
