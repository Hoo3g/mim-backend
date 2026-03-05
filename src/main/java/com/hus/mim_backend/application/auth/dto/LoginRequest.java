package com.hus.mim_backend.application.auth.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * Login request DTO
 */
@Setter
@Getter
public class LoginRequest {
    private String email;
    private String password;

    public LoginRequest() {
    }

    public LoginRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }

}
