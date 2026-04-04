package com.hus.mim_backend.application.news.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class NewsScheduleEntryDto {
    private String reportTime;
    private String reportRoom;
    private String reportFormat;
    private String paperTitle;
    private UUID paperId;
    private Integer displayOrder;
}
