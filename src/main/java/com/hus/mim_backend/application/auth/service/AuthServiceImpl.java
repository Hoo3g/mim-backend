package com.hus.mim_backend.application.auth.service;

import com.hus.mim_backend.application.auth.dto.AuthResponse;
import com.hus.mim_backend.application.auth.dto.GoogleLoginRequest;
import com.hus.mim_backend.application.auth.dto.LoginRequest;
import com.hus.mim_backend.application.auth.dto.RegisterRequest;
import com.hus.mim_backend.application.auth.dto.UserResponse;
import com.hus.mim_backend.application.auth.model.GoogleUserInfo;
import com.hus.mim_backend.application.auth.usecase.GoogleLoginUseCase;
import com.hus.mim_backend.application.auth.usecase.LoginUseCase;
import com.hus.mim_backend.application.auth.usecase.LogoutUseCase;
import com.hus.mim_backend.application.auth.usecase.RefreshTokenUseCase;
import com.hus.mim_backend.application.auth.usecase.RegisterUseCase;
import com.hus.mim_backend.application.port.output.GoogleTokenVerifier;
import com.hus.mim_backend.application.port.output.PasswordEncoder;
import com.hus.mim_backend.application.port.output.RefreshTokenRepository;
import com.hus.mim_backend.application.port.output.TokenProvider;
import com.hus.mim_backend.application.port.output.UserRepository;
import com.hus.mim_backend.domain.auth.model.Email;
import com.hus.mim_backend.domain.auth.model.RefreshToken;
import com.hus.mim_backend.domain.auth.model.User;
import com.hus.mim_backend.domain.shared.AuthException;
import com.hus.mim_backend.domain.shared.DomainException;

import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Auth Service Implementation - orchestrates authentication use cases
 * NOTE: No @Service or @Transactional here - framework agnostic
 */
public class AuthServiceImpl
        implements LoginUseCase, RegisterUseCase, RefreshTokenUseCase, LogoutUseCase, GoogleLoginUseCase {

    private static final Set<String> ALLOWED_USER_TYPES = Set.of("STUDENT", "LECTURER", "COMPANY", "ADMIN");
    private static final Pattern STUDENT_CODE_PATTERN = Pattern.compile("^[A-Z0-9]{6,20}$");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final GoogleTokenVerifier googleTokenVerifier;

    public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder,
            TokenProvider tokenProvider, RefreshTokenRepository refreshTokenRepository,
            GoogleTokenVerifier googleTokenVerifier) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.refreshTokenRepository = refreshTokenRepository;
        this.googleTokenVerifier = googleTokenVerifier;
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        if (request == null || request.getPassword() == null || request.getPassword().isBlank()) {
            throw new DomainException("Identifier and password are required");
        }

        String identifier = resolveIdentifier(request);

        User user = findUserByIdentifier(identifier)
                .orElseThrow(() -> new AuthException("Invalid credentials"));

        if (user.getPassword() == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AuthException("Invalid credentials");
        }

        ensureUserCanAuthenticate(user);
        return issueTokens(user);
    }

    @Override
    public UserResponse register(RegisterRequest request) {
        if (request == null || request.getEmail() == null || request.getPassword() == null) {
            throw new DomainException("Email and password are required");
        }

        Email email = new Email(request.getEmail());
        String normalizedUserType = normalizeUserType(request.getUserType());
        String normalizedStudentCode = null;

        if (userRepository.existsByEmail(email)) {
            throw new DomainException("Email already in use");
        }

        if ("STUDENT".equals(normalizedUserType)) {
            normalizedStudentCode = validateStudentCode(request.getStudentId(), true);
            if (userRepository.existsByStudentCode(normalizedStudentCode)) {
                throw new DomainException("Student code already in use");
            }
        }

        String encryptedPassword = passwordEncoder.encode(request.getPassword());

        User newUser = User.createNew(email, encryptedPassword, normalizedUserType);

        User savedUser = userRepository.save(newUser);

        if (normalizedStudentCode != null) {
            try {
                userRepository.upsertStudentCode(savedUser.getId(), normalizedStudentCode);
            } catch (RuntimeException ex) {
                throw new DomainException("Student code already in use");
            }
        }

        return UserResponse.fromDomain(savedUser);
    }

    @Override
    public AuthResponse refreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new AuthException("Refresh token is required");
        }

        if (!tokenProvider.validateToken(refreshToken)) {
            throw new AuthException("Invalid or expired refresh token");
        }

        RefreshToken storedToken = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new AuthException("Refresh token not found"));

        if (!storedToken.isActive()) {
            throw new AuthException("Refresh token has been revoked or expired");
        }

        User user = userRepository.findById(storedToken.getUserId())
                .orElseThrow(() -> new AuthException("User not found"));
        ensureUserCanAuthenticate(user);

        // Token rotation: old refresh token cannot be reused
        storedToken.revoke();
        refreshTokenRepository.save(storedToken);

        return issueTokens(user);
    }

    @Override
    public void logout(String token) {
        if (token == null || token.isBlank()) {
            throw new AuthException("Refresh token is required");
        }

        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new AuthException("Refresh token not found"));

        if (!refreshToken.isRevoked()) {
            refreshToken.revoke();
            refreshTokenRepository.save(refreshToken);
        }
    }

    @Override
    public AuthResponse loginWithGoogle(GoogleLoginRequest request) {
        if (request == null || request.getIdToken() == null || request.getIdToken().isBlank()) {
            throw new DomainException("Google ID token is required");
        }

        GoogleUserInfo googleUser = googleTokenVerifier.verifyIdToken(request.getIdToken());
        Email email = new Email(googleUser.email());
        String userType = normalizeUserType(request.getUserType());

        User user = userRepository.findByEmail(email)
                .orElseGet(() -> createUserFromGoogle(email, userType, googleUser.pictureUrl()));

        // Always sync avatar from Google when claim exists to avoid stale/broken URL.
        if (googleUser.pictureUrl() != null && !googleUser.pictureUrl().isBlank()
                && !googleUser.pictureUrl().equals(user.getAvatarUrl())) {
            user.setAvatarUrl(googleUser.pictureUrl());
            user = userRepository.save(user);
        }

        ensureUserCanAuthenticate(user);
        return issueTokens(user);
    }

    private User createUserFromGoogle(Email email, String userType, String pictureUrl) {
        String randomPassword = passwordEncoder.encode(UUID.randomUUID().toString());
        User user = User.createNew(email, randomPassword, userType);
        user.setAvatarUrl(pictureUrl);
        return userRepository.save(user);
    }

    private void ensureUserCanAuthenticate(User user) {
        if (!user.isActive()) {
            throw new DomainException("Account is pending approval or has been suspended");
        }
    }

    private AuthResponse issueTokens(User user) {
        // Multi-device support: do not revoke all sessions on each login.
        refreshTokenRepository.deleteExpiredTokens();

        String accessToken = tokenProvider.generateAccessToken(user);
        String refreshToken = tokenProvider.generateRefreshToken(user);

        RefreshToken tokenEntity = RefreshToken.issue(
                user.getId(),
                refreshToken,
                tokenProvider.getExpiryFromToken(refreshToken));
        refreshTokenRepository.save(tokenEntity);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(UserResponse.fromDomain(user))
                .build();
    }

    private String resolveIdentifier(LoginRequest request) {
        if (request.getIdentifier() != null && !request.getIdentifier().isBlank()) {
            return request.getIdentifier().trim();
        }
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            return request.getEmail().trim();
        }
        throw new DomainException("Identifier and password are required");
    }

    private java.util.Optional<User> findUserByIdentifier(String identifier) {
        if (identifier.contains("@")) {
            Email email = new Email(identifier);
            return userRepository.findByEmail(email);
        }

        String studentCode = validateStudentCode(identifier, false);
        return userRepository.findByStudentCode(studentCode)
                .filter(this::isStudentAccount);
    }

    private boolean isStudentAccount(User user) {
        return user.getRoles() != null && user.getRoles().stream()
                .anyMatch(role -> "STUDENT".equalsIgnoreCase(role));
    }

    private String validateStudentCode(String studentCode, boolean required) {
        if (studentCode == null || studentCode.isBlank()) {
            if (required) {
                throw new DomainException("Student code is required for student registration");
            }
            throw new DomainException("Invalid student code format");
        }

        String normalized = studentCode.trim().toUpperCase();
        if (!STUDENT_CODE_PATTERN.matcher(normalized).matches()) {
            throw new DomainException("Invalid student code format");
        }
        return normalized;
    }

    private String normalizeUserType(String userType) {
        String normalized = userType == null || userType.isBlank()
                ? "STUDENT"
                : userType.trim().toUpperCase();

        if (!ALLOWED_USER_TYPES.contains(normalized)) {
            throw new DomainException("Unsupported user type: " + normalized);
        }

        return normalized;
    }
}
