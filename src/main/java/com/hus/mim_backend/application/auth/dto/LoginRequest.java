package com.hus.mim_backend.application.auth.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * Login request DTO
 */
@Setter
@Getter
public class LoginRequest {
    /**
     * New canonical login identifier:
     * - email (contains '@'), or
     * - student code (alphanumeric, 6-20 chars)
     */
    private String identifier;

    /**
     * Backward-compatible field for legacy clients.
     */
    private String email;

    private String password;

    public LoginRequest() {
    }

    public LoginRequest(String identifier, String password) {
        this.identifier = identifier;
        this.password = password;
    }

    public LoginRequest(String identifier, String email, String password) {
        this.identifier = identifier;
        this.email = email;
        this.password = password;
    }

}
