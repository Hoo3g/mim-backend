package com.hus.mim_backend.application.news.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class NewsScheduleImportPreviewResponse {
    private String sourceUrl;
    private int totalEntries;
    private int matchedEntries;
    private int unmatchedEntries;
    private List<NewsScheduleEntryDto> entries;
}
