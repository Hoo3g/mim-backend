package com.hus.mim_backend.application.port.output;

import com.hus.mim_backend.domain.auth.model.User;

/**
 * Output port for token operations (JWT)
 */
public interface TokenProvider {
    String generateAccessToken(User user);

    String generateRefreshToken(User user);

    boolean validateToken(String token);

    String getEmailFromToken(String token);
}
