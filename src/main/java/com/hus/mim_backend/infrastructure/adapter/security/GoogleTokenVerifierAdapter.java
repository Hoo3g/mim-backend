package com.hus.mim_backend.infrastructure.adapter.security;

import com.hus.mim_backend.application.auth.model.GoogleUserInfo;
import com.hus.mim_backend.application.port.output.GoogleTokenVerifier;
import com.hus.mim_backend.domain.shared.DomainException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.Collection;
import java.util.Set;

/**
 * Verifies Google ID tokens using Google's public JWK set.
 */
@Component
public class GoogleTokenVerifierAdapter implements GoogleTokenVerifier {

    private static final Set<String> GOOGLE_ISSUERS = Set.of("https://accounts.google.com", "accounts.google.com");

    private final JwtDecoder jwtDecoder = NimbusJwtDecoder
            .withJwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
            .build();

    @Value("${google.oauth.client-id:}")
    private String googleClientId;

    @Override
    public GoogleUserInfo verifyIdToken(String idToken) {
        if (idToken == null || idToken.isBlank()) {
            throw new DomainException("Google ID token is required");
        }

        Jwt jwt;
        try {
            jwt = jwtDecoder.decode(idToken);
        } catch (JwtException ex) {
            throw new DomainException("Invalid Google ID token");
        }

        validateIssuer(jwt);
        validateAudience(jwt);

        String email = jwt.getClaimAsString("email");
        if (email == null || email.isBlank()) {
            throw new DomainException("Google token does not contain email");
        }

        boolean emailVerified = parseBoolean(jwt.getClaim("email_verified"));
        if (!emailVerified) {
            throw new DomainException("Google account email is not verified");
        }

        return new GoogleUserInfo(
                email,
                jwt.getClaimAsString("name"),
                jwt.getClaimAsString("picture"),
                true);
    }

    private void validateIssuer(Jwt jwt) {
        URL issuerUrl = jwt.getIssuer();
        String issuer = issuerUrl != null ? issuerUrl.toString() : jwt.getClaimAsString("iss");
        if (issuer == null || !GOOGLE_ISSUERS.contains(issuer)) {
            throw new DomainException("Invalid Google token issuer");
        }
    }

    private void validateAudience(Jwt jwt) {
        if (googleClientId == null || googleClientId.isBlank()) {
            throw new DomainException("Google OAuth client ID is not configured");
        }

        Collection<String> audiences = jwt.getAudience();
        if (audiences == null || audiences.stream().noneMatch(googleClientId::equals)) {
            throw new DomainException("Google token audience mismatch");
        }
    }

    private boolean parseBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof String str) {
            return Boolean.parseBoolean(str);
        }
        return false;
    }
}
