package com.hus.mim_backend.application.post.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UpdatePostRequest {
    private String title;
    private String description;
    private String status;
    private List<String> tags;
}
