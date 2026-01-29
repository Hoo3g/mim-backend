package com.hus.mim_backend.application.auth.dto;

/**
 * Authentication response DTO
 */
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private UserResponse user;

    public AuthResponse() {
    }

    // Getters and Setters
    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public UserResponse getUser() {
        return user;
    }

    public void setUser(UserResponse user) {
        this.user = user;
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
