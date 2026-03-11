package com.hus.mim_backend.domain.auth.model;

/**
 * Email Value Object - Self-validating immutable value object
 */
public record Email(String value) {
    public Email {
        if (value == null || !value.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new IllegalArgumentException("Invalid email format");
        }
    }

}
