package com.hus.mim_backend.domain.auth.model;

import lombok.Getter;

/**
 * Password Value Object - Self-validating immutable value object
 */
@Getter
public class Password {
    private final String value;

    public Password(String value) {
        if (value == null || value.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long");
        }
        this.value = value;
    }
}
