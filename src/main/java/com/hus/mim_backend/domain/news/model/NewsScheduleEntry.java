package com.hus.mim_backend.domain.news.model;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class NewsScheduleEntry {
    private String reportTime;
    private String reportRoom;
    private String reportFormat;
    private String paperTitle;
    private UUID paperId;
    private Integer displayOrder;
}
