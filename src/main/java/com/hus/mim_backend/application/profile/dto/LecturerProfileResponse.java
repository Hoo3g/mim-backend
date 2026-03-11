package com.hus.mim_backend.application.profile.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class LecturerProfileResponse {
    private UUID id;
    private String firstName;
    private String lastName;
    private String title;
    private String academicRank;
    private String bio;
    private List<String> researchInterests;
}
