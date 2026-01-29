package com.hus.mim_backend.application.auth.dto;

import com.hus.mim_backend.domain.auth.model.User;
import java.util.Set;

/**
 * User response DTO
 */
public class UserResponse {
    private String id;
    private String email;
    private String status;
    private Set<String> roles;

    public UserResponse() {
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    public static UserResponse fromDomain(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId().toString());
        response.setEmail(user.getEmail().getValue());
        response.setStatus(user.getStatus().name());
        response.setRoles(user.getRoles());
        return response;
    }

    public static UserResponseBuilder builder() {
        return new UserResponseBuilder();
    }

    public static class UserResponseBuilder {
        private final UserResponse response = new UserResponse();

        public UserResponseBuilder id(String id) {
            response.setId(id);
            return this;
        }

        public UserResponseBuilder email(String email) {
            response.setEmail(email);
            return this;
        }

        public UserResponseBuilder status(String status) {
            response.setStatus(status);
            return this;
        }

        public UserResponseBuilder roles(Set<String> roles) {
            response.setRoles(roles);
            return this;
        }

        public UserResponse build() {
            return response;
        }
    }
}
