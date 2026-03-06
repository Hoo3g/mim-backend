package com.hus.mim_backend.infrastructure.adapter.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

/**
 * Handles refresh token transport via HttpOnly cookie.
 */
@Component
public class RefreshTokenCookieService {

    private final String cookieName;
    private final String cookiePath;
    private final String sameSite;
    private final boolean secureCookie;
    private final long refreshTokenExpirationMs;

    public RefreshTokenCookieService(
            @Value("${auth.refresh-cookie.name:refresh_token}") String cookieName,
            @Value("${auth.refresh-cookie.path:/api/v1/auth}") String cookiePath,
            @Value("${auth.refresh-cookie.same-site:Lax}") String sameSite,
            @Value("${auth.refresh-cookie.secure:false}") boolean secureCookie,
            @Value("${jwt.refreshTokenExpiration}") long refreshTokenExpirationMs) {
        this.cookieName = cookieName;
        this.cookiePath = cookiePath;
        this.sameSite = sameSite;
        this.secureCookie = secureCookie;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    public void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        long maxAgeSeconds = Math.max(1L, Duration.ofMillis(refreshTokenExpirationMs).getSeconds());
        ResponseCookie cookie = ResponseCookie.from(cookieName, refreshToken)
                .httpOnly(true)
                .secure(secureCookie)
                .path(cookiePath)
                .sameSite(sameSite)
                .maxAge(maxAgeSeconds)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public void clearRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(cookieName, "")
                .httpOnly(true)
                .secure(secureCookie)
                .path(cookiePath)
                .sameSite(sameSite)
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public Optional<String> extractRefreshToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null || cookies.length == 0) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(cookie -> cookieName.equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> value != null && !value.isBlank())
                .findFirst();
    }
}
