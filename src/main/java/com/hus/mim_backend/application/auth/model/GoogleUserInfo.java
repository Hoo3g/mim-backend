package com.hus.mim_backend.application.auth.model;

/**
 * Canonical user info extracted from a verified Google ID token.
 */
public record GoogleUserInfo(String email, String name, String pictureUrl, boolean emailVerified) {
}
