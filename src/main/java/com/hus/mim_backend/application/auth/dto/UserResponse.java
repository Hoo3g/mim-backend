package com.hus.mim_backend.application.auth.dto;

import com.hus.mim_backend.domain.auth.model.User;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

/**
 * User response DTO
 */
@Setter
@Getter
public class UserResponse {
    // Getters and Setters
    private String id;
    private String email;
    private String status;
    private Set<String> roles;

    public UserResponse() {
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
