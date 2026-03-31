package com.hus.mim_backend.infrastructure.adapter.web.auth;

import com.hus.mim_backend.application.auth.dto.AuthResponse;
import com.hus.mim_backend.application.auth.dto.GoogleLoginRequest;
import com.hus.mim_backend.application.auth.dto.LoginRequest;
import com.hus.mim_backend.application.auth.dto.RegisterRequest;
import com.hus.mim_backend.application.auth.dto.UserResponse;
import com.hus.mim_backend.application.auth.dto.VerifyEmailRequest;
import com.hus.mim_backend.application.auth.usecase.GoogleLoginUseCase;
import com.hus.mim_backend.application.auth.usecase.LoginUseCase;
import com.hus.mim_backend.application.auth.usecase.LogoutUseCase;
import com.hus.mim_backend.application.auth.usecase.RefreshTokenUseCase;
import com.hus.mim_backend.application.auth.usecase.RegisterUseCase;
import com.hus.mim_backend.application.auth.usecase.ResendEmailVerificationUseCase;
import com.hus.mim_backend.application.auth.usecase.VerifyEmailUseCase;
import com.hus.mim_backend.application.rbac.usecase.ManageRbacUseCase;
import com.hus.mim_backend.domain.shared.AuthException;
import com.hus.mim_backend.domain.shared.DomainException;
import com.hus.mim_backend.infrastructure.adapter.security.RefreshTokenCookieService;
import com.hus.mim_backend.shared.api.ApiResponse;
import com.hus.mim_backend.shared.constants.ApiEndpoints;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;
import java.util.UUID;

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
    private final VerifyEmailUseCase verifyEmailUseCase;
    private final ResendEmailVerificationUseCase resendEmailVerificationUseCase;
    private final RefreshTokenCookieService refreshTokenCookieService;
    private final ManageRbacUseCase manageRbacUseCase;
    private final boolean googleLoginEnabled;

    public AuthController(LoginUseCase loginUseCase, GoogleLoginUseCase googleLoginUseCase,
            RegisterUseCase registerUseCase, RefreshTokenUseCase refreshTokenUseCase,
            LogoutUseCase logoutUseCase, VerifyEmailUseCase verifyEmailUseCase,
            ResendEmailVerificationUseCase resendEmailVerificationUseCase,
            RefreshTokenCookieService refreshTokenCookieService,
            ManageRbacUseCase manageRbacUseCase,
            @Value("${app.auth.google-login.enabled:false}") boolean googleLoginEnabled) {
        this.loginUseCase = loginUseCase;
        this.googleLoginUseCase = googleLoginUseCase;
        this.registerUseCase = registerUseCase;
        this.refreshTokenUseCase = refreshTokenUseCase;
        this.logoutUseCase = logoutUseCase;
        this.verifyEmailUseCase = verifyEmailUseCase;
        this.resendEmailVerificationUseCase = resendEmailVerificationUseCase;
        this.refreshTokenCookieService = refreshTokenCookieService;
        this.manageRbacUseCase = manageRbacUseCase;
        this.googleLoginEnabled = googleLoginEnabled;
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
        if (!googleLoginEnabled) {
            throw new DomainException("Google login is disabled");
        }
        AuthResponse authResponse = googleLoginUseCase.loginWithGoogle(request);
        attachRefreshCookieAndSanitizeResponse(authResponse, response);
        return ResponseEntity.ok(ApiResponse.success(authResponse,
                "Google login successful"));
    }

    @PostMapping(ApiEndpoints.REGISTER)
    public ResponseEntity<ApiResponse<UserResponse>> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(ApiResponse.success(registerUseCase.register(request), "Registration successful"));
    }

    @PostMapping(ApiEndpoints.VERIFY_EMAIL)
    public ResponseEntity<ApiResponse<UserResponse>> verifyEmail(@RequestBody VerifyEmailRequest request) {
        if (request == null || request.getToken() == null || request.getToken().isBlank()) {
            throw new DomainException("Verification token is required");
        }
        UserResponse user = verifyEmailUseCase.verifyEmail(request.getToken());
        return ResponseEntity.ok(ApiResponse.success(user, "Email verified successfully"));
    }

    @PostMapping(ApiEndpoints.RESEND_VERIFY_EMAIL)
    public ResponseEntity<ApiResponse<Void>> resendVerifyEmail(Authentication authentication) {
        String email = resolveAuthenticatedEmail(authentication);
        resendEmailVerificationUseCase.resendEmailVerification(email);
        return ResponseEntity.ok(ApiResponse.success(null, "Verification email sent"));
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
        enrichPermissions(authResponse);
        if (authResponse.getRefreshToken() != null && !authResponse.getRefreshToken().isBlank()) {
            refreshTokenCookieService.addRefreshTokenCookie(response, authResponse.getRefreshToken());
        }
        authResponse.setRefreshToken(null);
    }

    private void enrichPermissions(AuthResponse authResponse) {
        if (authResponse == null || authResponse.getUser() == null || authResponse.getUser().getId() == null) {
            return;
        }

        try {
            UUID userId = UUID.fromString(authResponse.getUser().getId());
            Set<String> permissions = manageRbacUseCase.getEffectivePermissionsByUserId(userId);
            authResponse.getUser().setPermissions(permissions);
        } catch (RuntimeException ex) {
            authResponse.getUser().setPermissions(Set.of());
        }
    }

    private String resolveAuthenticatedEmail(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AuthException("Authentication required");
        }
        String email = String.valueOf(authentication.getPrincipal());
        if (!StringUtils.hasText(email)) {
            throw new AuthException("Authentication required");
        }
        return email.trim();
    }
}
