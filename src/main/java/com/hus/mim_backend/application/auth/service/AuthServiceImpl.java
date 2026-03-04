package com.hus.mim_backend.application.auth.service;

import com.hus.mim_backend.application.auth.dto.AuthResponse;
import com.hus.mim_backend.application.auth.dto.LoginRequest;
import com.hus.mim_backend.application.auth.dto.RegisterRequest;
import com.hus.mim_backend.application.auth.dto.UserResponse;
import com.hus.mim_backend.application.auth.usecase.LoginUseCase;
import com.hus.mim_backend.application.auth.usecase.LogoutUseCase;
import com.hus.mim_backend.application.auth.usecase.RefreshTokenUseCase;
import com.hus.mim_backend.application.auth.usecase.RegisterUseCase;
import com.hus.mim_backend.application.port.output.PasswordEncoder;
import com.hus.mim_backend.application.port.output.TokenProvider;
import com.hus.mim_backend.application.port.output.UserRepository;
import com.hus.mim_backend.domain.auth.model.Email;
import com.hus.mim_backend.domain.auth.model.User;
import com.hus.mim_backend.domain.shared.DomainException;

/**
 * Auth Service Implementation - orchestrates authentication use cases
 * NOTE: No @Service or @Transactional here - framework agnostic
 */
public class AuthServiceImpl implements LoginUseCase, RegisterUseCase, RefreshTokenUseCase, LogoutUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;

    public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder,
            TokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        // Validate input via Value Object
        Email email = new Email(request.getEmail());

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new DomainException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new DomainException("Invalid credentials");
        }

        if (!user.isActive()) {
            throw new DomainException("Account is pending approval or has been suspended");
        }

        String accessToken = tokenProvider.generateAccessToken(user);
        String refreshToken = tokenProvider.generateRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(UserResponse.fromDomain(user))
                .build();
    }

    @Override
    public UserResponse register(RegisterRequest request) {
        Email email = new Email(request.getEmail());

        if (userRepository.existsByEmail(email)) {
            throw new DomainException("Email already in use");
        }

        String encryptedPassword = passwordEncoder.encode(request.getPassword());

        User newUser = User.createNew(email, encryptedPassword, request.getUserType());

        User savedUser = userRepository.save(newUser);

        return UserResponse.fromDomain(savedUser);
    }

    @Override
    public AuthResponse refreshToken(String refreshToken) {
        if (!tokenProvider.validateToken(refreshToken)) {
            throw new DomainException("Invalid or expired refresh token");
        }

        String emailVal = tokenProvider.getEmailFromToken(refreshToken);
        User user = userRepository.findByEmail(new Email(emailVal))
                .orElseThrow(() -> new DomainException("User not found"));

        String newAccessToken = tokenProvider.generateAccessToken(user);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .user(UserResponse.fromDomain(user))
                .build();
    }

    @Override
    public void logout(String token) {
        // TODO: Implement token blacklisting via RefreshTokenRepository
    }
}
