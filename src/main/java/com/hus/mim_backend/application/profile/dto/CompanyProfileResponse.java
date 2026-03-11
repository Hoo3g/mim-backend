package com.hus.mim_backend.application.profile.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CompanyProfileResponse {
    private UUID id;
    private String name;
    private String industry;
    private String website;
    private String location;
    private String description;
}
