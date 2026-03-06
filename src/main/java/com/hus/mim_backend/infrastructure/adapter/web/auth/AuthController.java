package com.hus.mim_backend.infrastructure.adapter.web.auth;

import com.hus.mim_backend.application.auth.dto.AuthResponse;
import com.hus.mim_backend.application.auth.dto.GoogleLoginRequest;
import com.hus.mim_backend.application.auth.dto.LoginRequest;
import com.hus.mim_backend.application.auth.dto.RegisterRequest;
import com.hus.mim_backend.application.auth.dto.UserResponse;
import com.hus.mim_backend.application.auth.usecase.GoogleLoginUseCase;
import com.hus.mim_backend.application.auth.usecase.LoginUseCase;
import com.hus.mim_backend.application.auth.usecase.LogoutUseCase;
import com.hus.mim_backend.application.auth.usecase.RefreshTokenUseCase;
import com.hus.mim_backend.application.auth.usecase.RegisterUseCase;
import com.hus.mim_backend.domain.shared.AuthException;
import com.hus.mim_backend.infrastructure.adapter.security.RefreshTokenCookieService;
import com.hus.mim_backend.shared.api.ApiResponse;
import com.hus.mim_backend.shared.constants.ApiEndpoints;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Web adapter for authentication endpoints
 */
@RestController
@RequestMapping(ApiEndpoints.AUTH)
public class AuthController {

    private final LoginUseCase loginUseCase;
    private final GoogleLoginUseCase googleLoginUseCase;
    private final RegisterUseCase registerUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final LogoutUseCase logoutUseCase;
    private final RefreshTokenCookieService refreshTokenCookieService;

    public AuthController(LoginUseCase loginUseCase, GoogleLoginUseCase googleLoginUseCase,
            RegisterUseCase registerUseCase, RefreshTokenUseCase refreshTokenUseCase,
            LogoutUseCase logoutUseCase, RefreshTokenCookieService refreshTokenCookieService) {
        this.loginUseCase = loginUseCase;
        this.googleLoginUseCase = googleLoginUseCase;
        this.registerUseCase = registerUseCase;
        this.refreshTokenUseCase = refreshTokenUseCase;
        this.logoutUseCase = logoutUseCase;
        this.refreshTokenCookieService = refreshTokenCookieService;
    }

    @PostMapping(ApiEndpoints.LOGIN)
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @RequestBody LoginRequest request,
            HttpServletResponse response) {
        AuthResponse authResponse = loginUseCase.login(request);
        attachRefreshCookieAndSanitizeResponse(authResponse, response);
        return ResponseEntity.ok(ApiResponse.success(authResponse, "Login successful"));
    }

    @PostMapping(ApiEndpoints.GOOGLE_LOGIN)
    public ResponseEntity<ApiResponse<AuthResponse>> googleLogin(
            @RequestBody GoogleLoginRequest request,
            HttpServletResponse response) {
        AuthResponse authResponse = googleLoginUseCase.loginWithGoogle(request);
        attachRefreshCookieAndSanitizeResponse(authResponse, response);
        return ResponseEntity.ok(ApiResponse.success(authResponse,
                "Google login successful"));
    }

    @PostMapping(ApiEndpoints.REGISTER)
    public ResponseEntity<ApiResponse<UserResponse>> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(ApiResponse.success(registerUseCase.register(request), "Registration successful"));
    }

    @PostMapping(ApiEndpoints.REFRESH_TOKEN)
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            HttpServletRequest request,
            HttpServletResponse response) {
        String refreshToken = refreshTokenCookieService.extractRefreshToken(request)
                .orElseThrow(() -> new AuthException("Refresh token cookie is required"));
        AuthResponse authResponse = refreshTokenUseCase.refreshToken(refreshToken);
        attachRefreshCookieAndSanitizeResponse(authResponse, response);
        return ResponseEntity.ok(ApiResponse.success(authResponse, "Token refreshed"));
    }

    @PostMapping(ApiEndpoints.LOGOUT)
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest request,
            HttpServletResponse response) {
        refreshTokenCookieService.extractRefreshToken(request).ifPresent(logoutUseCase::logout);
        refreshTokenCookieService.clearRefreshTokenCookie(response);
        return ResponseEntity.ok(ApiResponse.success(null, "Logout successful"));
    }

    private void attachRefreshCookieAndSanitizeResponse(AuthResponse authResponse, HttpServletResponse response) {
        if (authResponse.getRefreshToken() != null && !authResponse.getRefreshToken().isBlank()) {
            refreshTokenCookieService.addRefreshTokenCookie(response, authResponse.getRefreshToken());
        }
        authResponse.setRefreshToken(null);
    }
}
