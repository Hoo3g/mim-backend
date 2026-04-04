package com.hus.mim_backend.application.news.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class UpdateNewsRequest {
    private String title;
    private String content;
    private String summary;
    private String imageUrl;
    private String status;
    private String contentType;
    private String importSourceUrl;
    private List<NewsScheduleEntryDto> scheduleEntries;
    private Boolean pinned;

}
