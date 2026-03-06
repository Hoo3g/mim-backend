package com.hus.mim_backend.domain.shared;

/**
 * Authentication/authorization failure mapped to HTTP 401.
 */
public class AuthException extends RuntimeException {

    public AuthException(String message) {
        super(message);
    }

    public AuthException(String message, Throwable cause) {
        super(message, cause);
    }
}
