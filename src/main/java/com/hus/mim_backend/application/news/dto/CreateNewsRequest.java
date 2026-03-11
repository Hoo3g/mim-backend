package com.hus.mim_backend.application.news.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class CreateNewsRequest {
    private String title;
    private String content;
    private String summary;
    private String imageUrl;
    private String status;
    private Boolean pinned;

}
