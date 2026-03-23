package com.hus.mim_backend.application.auth.usecase;

import java.util.UUID;

/**
 * Use case for resolving currently authenticated and verified user identity.
 */
public interface VerifiedAccountUseCase {
    UUID requireVerifiedUserId(String email);

    String requireVerifiedEmail(String email);
}
