package com.hus.mim_backend.application.auth.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * Authentication response DTO
 */
@Setter
@Getter
public class AuthResponse {
    // Getters and Setters
    private String accessToken;
    /**
     * Deprecated for clients. Server now sends refresh token via HttpOnly cookie.
     */
    private String refreshToken;
    private UserResponse user;

    public AuthResponse() {
    }

    public static AuthResponseBuilder builder() {
        return new AuthResponseBuilder();
    }

    public static class AuthResponseBuilder {
        private final AuthResponse response = new AuthResponse();

        public AuthResponseBuilder accessToken(String accessToken) {
            response.setAccessToken(accessToken);
            return this;
        }

        public AuthResponseBuilder refreshToken(String refreshToken) {
            response.setRefreshToken(refreshToken);
            return this;
        }

        public AuthResponseBuilder user(UserResponse user) {
            response.setUser(user);
            return this;
        }

        public AuthResponse build() {
            return response;
        }
    }
}
