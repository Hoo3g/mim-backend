package com.hus.mim_backend.application.auth.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * Google login request DTO.
 * idToken is issued by Google Identity Services on the frontend.
 */
@Getter
@Setter
public class GoogleLoginRequest {
    private String idToken;
    private String userType;
}
