package com.hus.mim_backend.application.post.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CreatePostRequest {
    private String title;
    private String description;
    private String requirements;
    private String benefits;
    private String postType;
    private String jobType;
    private String location;
    private String salaryRange;
    private List<String> tags;
}
