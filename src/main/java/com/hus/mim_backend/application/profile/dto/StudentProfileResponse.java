package com.hus.mim_backend.application.profile.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentProfileResponse {
    private UUID id;
    private String firstName;
    private String lastName;
    private String university;
    private String major;
    private String bio;
    private String cvUrl;
    private String studentType;
}
