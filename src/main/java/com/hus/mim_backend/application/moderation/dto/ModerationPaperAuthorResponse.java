package com.hus.mim_backend.application.moderation.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ModerationPaperAuthorResponse {
    private String authorId;
    private String name;
    private boolean mainAuthor;
    private int authorOrder;
}
