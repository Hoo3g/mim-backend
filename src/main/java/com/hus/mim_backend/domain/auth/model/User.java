package com.hus.mim_backend.domain.auth.model;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * User Aggregate Root - Central entity for authentication bounded context
 */
public class User {
    private UUID id;
    private Email email;
    private String password; // Encrypted password
    private String avatarUrl;
    private AccountStatus status;
    private Set<String> roles;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public User() {
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Email getEmail() {
        return email;
    }

    public void setEmail(Email email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Factory method: Creates a new user with business rules applied
     */
    public static User createNew(Email email, String encryptedPassword, String userType) {
        // Business rule: Students are auto-approved, others need approval
        AccountStatus initialStatus = "STUDENT".equals(userType)
                ? AccountStatus.APPROVED
                : AccountStatus.PENDING;

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(email);
        user.setPassword(encryptedPassword);
        user.setStatus(initialStatus);
        user.setRoles(Set.of(userType));
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }

    // Builder pattern
    public static UserBuilder builder() {
        return new UserBuilder();
    }

    /**
     * Domain logic: Check if user can authenticate
     */
    public boolean isActive() {
        return status == AccountStatus.APPROVED;
    }

    public static class UserBuilder {
        private final User user = new User();

        public UserBuilder id(UUID id) {
            user.setId(id);
            return this;
        }

        public UserBuilder email(Email email) {
            user.setEmail(email);
            return this;
        }

        public UserBuilder password(String password) {
            user.setPassword(password);
            return this;
        }

        public UserBuilder avatarUrl(String avatarUrl) {
            user.setAvatarUrl(avatarUrl);
            return this;
        }

        public UserBuilder status(AccountStatus status) {
            user.setStatus(status);
            return this;
        }

        public UserBuilder roles(Set<String> roles) {
            user.setRoles(roles);
            return this;
        }

        public UserBuilder createdAt(LocalDateTime createdAt) {
            user.setCreatedAt(createdAt);
            return this;
        }

        public UserBuilder updatedAt(LocalDateTime updatedAt) {
            user.setUpdatedAt(updatedAt);
            return this;
        }

        public User build() {
            return user;
        }
    }
}
