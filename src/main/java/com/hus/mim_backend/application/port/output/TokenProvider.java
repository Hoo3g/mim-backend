package com.hus.mim_backend.application.port.output;

import com.hus.mim_backend.domain.auth.model.User;

import java.util.Set;

/**
 * Output port for token operations (JWT)
 */
public interface TokenProvider {
    String generateAccessToken(User user);

    String generateRefreshToken(User user);

    boolean validateToken(String token);

    String getEmailFromToken(String token);

    /**
     * Extract roles from a JWT token claim.
     * Used by the security filter to populate GrantedAuthorities.
     */
    Set<String> getRolesFromToken(String token);
}
