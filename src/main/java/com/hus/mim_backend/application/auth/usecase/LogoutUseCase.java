package com.hus.mim_backend.application.auth.usecase;

/**
 * Input port for logout use case
 */
public interface LogoutUseCase {
    void logout(String token);
}
