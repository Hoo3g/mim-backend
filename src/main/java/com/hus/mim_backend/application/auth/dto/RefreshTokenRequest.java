package com.hus.mim_backend.application.auth.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * Refresh token request DTO.
 */
@Getter
@Setter
public class RefreshTokenRequest {
    private String refreshToken;
}
