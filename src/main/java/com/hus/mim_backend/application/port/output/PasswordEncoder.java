package com.hus.mim_backend.application.port.output;

/**
 * Output port for password encoding operations
 */
public interface PasswordEncoder {
    String encode(String rawPassword);

    boolean matches(String rawPassword, String encodedPassword);
}
