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
import com.hus.mim_backend.application.auth.usecase.ResendEmailVerificationUseCase;
import com.hus.mim_backend.application.auth.usecase.VerifyEmailUseCase;
import com.hus.mim_backend.application.port.output.EmailVerificationNotificationPort;
import com.hus.mim_backend.application.port.output.EmailVerificationTokenRepository;
import com.hus.mim_backend.application.port.output.GoogleTokenVerifier;
import com.hus.mim_backend.application.port.output.PasswordEncoder;
import com.hus.mim_backend.application.port.output.RefreshTokenRepository;
import com.hus.mim_backend.application.port.output.TokenProvider;
import com.hus.mim_backend.application.port.output.UserRepository;
import com.hus.mim_backend.domain.auth.model.AccountStatus;
import com.hus.mim_backend.domain.auth.model.Email;
import com.hus.mim_backend.domain.auth.model.EmailVerificationToken;
import com.hus.mim_backend.domain.auth.model.RefreshToken;
import com.hus.mim_backend.domain.auth.model.User;
import com.hus.mim_backend.domain.shared.AuthException;
import com.hus.mim_backend.domain.shared.DomainException;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Auth Service Implementation - orchestrates authentication use cases
 * NOTE: No @Service or @Transactional here - framework agnostic
 */
public class AuthServiceImpl
        implements LoginUseCase, RegisterUseCase, RefreshTokenUseCase, LogoutUseCase, GoogleLoginUseCase,
        VerifyEmailUseCase, ResendEmailVerificationUseCase {

    private static final Set<String> ALLOWED_USER_TYPES = Set.of("STUDENT", "LECTURER", "COMPANY", "ADMIN");
    private static final Pattern STUDENT_CODE_PATTERN = Pattern.compile("^[A-Z0-9]{6,20}$");
    private static final String GOOGLE_ONBOARDING_REQUIRED = "GOOGLE_ONBOARDING_REQUIRED";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final GoogleTokenVerifier googleTokenVerifier;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final EmailVerificationNotificationPort emailVerificationNotificationPort;
    private final long emailVerificationTokenTtlMinutes;

    public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder,
            TokenProvider tokenProvider, RefreshTokenRepository refreshTokenRepository,
            GoogleTokenVerifier googleTokenVerifier,
            EmailVerificationTokenRepository emailVerificationTokenRepository,
            EmailVerificationNotificationPort emailVerificationNotificationPort,
            long emailVerificationTokenTtlMinutes) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.refreshTokenRepository = refreshTokenRepository;
        this.googleTokenVerifier = googleTokenVerifier;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.emailVerificationNotificationPort = emailVerificationNotificationPort;
        this.emailVerificationTokenTtlMinutes = Math.max(5, emailVerificationTokenTtlMinutes);
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
        LecturerRegistration lecturerRegistration = null;
        String companyRegistrationName = null;
        String registrationDisplayName = normalizeText(request.getFullName());

        if (userRepository.existsByEmail(email)) {
            throw new DomainException("Email already in use");
        }

        if ("STUDENT".equals(normalizedUserType)) {
            normalizedStudentCode = validateStudentCode(request.getStudentId(), true);
            if (userRepository.existsByStudentCode(normalizedStudentCode)) {
                throw new DomainException("Student code already in use");
            }
        } else if ("LECTURER".equals(normalizedUserType)) {
            lecturerRegistration = validateLecturerRegistration(request.getFullName(), request.getTitle());
        } else if ("COMPANY".equals(normalizedUserType)) {
            companyRegistrationName = validateCompanyRegistration(request.getCompanyName(), request.getFullName());
            registrationDisplayName = companyRegistrationName;
        }

        String encryptedPassword = passwordEncoder.encode(request.getPassword());

        User newUser = User.createNew(email, encryptedPassword, normalizedUserType, AccountStatus.PENDING);
        newUser.setFullName(registrationDisplayName);

        User savedUser = userRepository.save(newUser);

        if (normalizedStudentCode != null) {
            try {
                userRepository.upsertStudentCode(savedUser.getId(), normalizedStudentCode);
            } catch (RuntimeException ex) {
                throw new DomainException("Student code already in use");
            }
        }

        if (lecturerRegistration != null) {
            userRepository.upsertLecturerRegistration(
                    savedUser.getId(),
                    lecturerRegistration.firstName(),
                    lecturerRegistration.lastName(),
                    lecturerRegistration.title());
        }

        if (companyRegistrationName != null) {
            userRepository.upsertCompanyRegistration(savedUser.getId(), companyRegistrationName, null);
        }

        issueEmailVerification(savedUser);
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
        User user = userRepository.findByEmail(email)
                .map(existingUser -> completeGoogleOnboardingIfNeeded(existingUser, googleUser, request))
                .orElseGet(() -> createGoogleOnboardedUser(email, googleUser, request));

        // Always sync avatar from Google when claim exists to avoid stale/broken URL.
        if (googleUser.pictureUrl() != null && !googleUser.pictureUrl().isBlank()
                && !googleUser.pictureUrl().equals(user.getAvatarUrl())) {
            user.setAvatarUrl(googleUser.pictureUrl());
            user = userRepository.save(user);
        }

        if (normalizeText(user.getFullName()) == null) {
            String resolvedFullName = normalizeText(googleUser.name());
            if (resolvedFullName != null) {
                user.setFullName(resolvedFullName);
                user = userRepository.save(user);
            }
        }

        if (googleUser.emailVerified() && user.getStatus() != AccountStatus.APPROVED) {
            user.setStatus(AccountStatus.APPROVED);
            user.setUpdatedAt(LocalDateTime.now());
            user = userRepository.save(user);
            emailVerificationTokenRepository.deleteByUserId(user.getId());
        }

        ensureUserCanAuthenticate(user);
        return issueTokens(user);
    }

    @Override
    public UserResponse verifyEmail(String token) {
        String normalizedToken = token == null ? "" : token.trim();
        if (normalizedToken.isEmpty()) {
            throw new DomainException("Verification token is required");
        }

        emailVerificationTokenRepository.deleteExpiredTokens();

        EmailVerificationToken verificationToken = emailVerificationTokenRepository.findByToken(normalizedToken)
                .orElseThrow(() -> new DomainException("Verification link is invalid or has expired"));

        if (verificationToken.isExpired()) {
            emailVerificationTokenRepository.deleteByToken(normalizedToken);
            throw new DomainException("Verification link is invalid or has expired");
        }

        User user = userRepository.findById(verificationToken.getUserId())
                .orElseThrow(() -> new DomainException("Account not found"));

        if (user.getStatus() == AccountStatus.BLOCKED) {
            throw new DomainException("Account has been blocked");
        }

        user.setStatus(AccountStatus.APPROVED);
        user.setUpdatedAt(LocalDateTime.now());
        User savedUser = userRepository.save(user);
        emailVerificationTokenRepository.deleteByUserId(savedUser.getId());
        return UserResponse.fromDomain(savedUser);
    }

    @Override
    public void resendEmailVerification(String email) {
        if (email == null || email.isBlank()) {
            throw new AuthException("Authentication required");
        }

        User user = userRepository.findByEmail(new Email(email.trim()))
                .orElseThrow(() -> new AuthException("Authenticated user is not found"));

        if (user.getStatus() == AccountStatus.BLOCKED) {
            throw new DomainException("Account has been blocked");
        }
        if (user.getStatus() == AccountStatus.APPROVED) {
            return;
        }

        issueEmailVerification(user);
    }

    private User createGoogleOnboardedUser(Email email, GoogleUserInfo googleUser, GoogleLoginRequest request) {
        GoogleOnboardingDetails details = resolveGoogleOnboardingDetails(request, null);
        User user = createUserFromGoogle(email, details.userType(), pictureUrlFromGoogle(googleUser), details.fullName());
        upsertRegistrationProfile(user.getId(), details);
        return user;
    }

    private User createUserFromGoogle(Email email, String userType, String pictureUrl, String fullName) {
        String randomPassword = passwordEncoder.encode(UUID.randomUUID().toString());
        User user = User.createNew(email, randomPassword, userType, AccountStatus.APPROVED);
        user.setFullName(fullName);
        user.setAvatarUrl(pictureUrl);
        return userRepository.save(user);
    }

    private User completeGoogleOnboardingIfNeeded(User user, GoogleUserInfo googleUser, GoogleLoginRequest request) {
        if (hasCompletedRegistrationProfile(user)) {
            return user;
        }

        GoogleOnboardingDetails details = resolveGoogleOnboardingDetails(request, user.getId());

        user.setRoles(Set.of(details.userType()));
        user.setFullName(details.fullName());
        if (pictureUrlFromGoogle(googleUser) != null && !pictureUrlFromGoogle(googleUser).isBlank()) {
            user.setAvatarUrl(pictureUrlFromGoogle(googleUser));
        }
        user.setUpdatedAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);
        upsertRegistrationProfile(savedUser.getId(), details);
        return savedUser;
    }

    private boolean hasCompletedRegistrationProfile(User user) {
        if (user == null || user.getId() == null) {
            return false;
        }

        String currentRole = primaryRole(user);
        if (currentRole == null) {
            return false;
        }

        return switch (currentRole) {
            case "STUDENT" -> userRepository.hasStudentRegistration(user.getId());
            case "LECTURER" -> userRepository.hasLecturerRegistration(user.getId());
            case "COMPANY" -> userRepository.hasCompanyRegistration(user.getId());
            default -> true;
        };
    }

    private String primaryRole(User user) {
        if (user == null || user.getRoles() == null || user.getRoles().isEmpty()) {
            return null;
        }
        return user.getRoles().stream()
                .filter(role -> role != null && !role.isBlank())
                .map(role -> role.trim().toUpperCase())
                .findFirst()
                .orElse(null);
    }

    private void upsertRegistrationProfile(UUID userId, GoogleOnboardingDetails details) {
        if (userId == null || details == null) {
            return;
        }

        switch (details.userType()) {
            case "STUDENT" -> {
                String[] splitName = splitFullName(details.fullName());
                try {
                    userRepository.upsertStudentCode(userId, details.studentCode());
                } catch (RuntimeException ex) {
                    throw new DomainException("Student code already in use");
                }
                userRepository.upsertStudentRegistration(userId, splitName[0], splitName[1], details.studentFaculty());
            }
            case "LECTURER" -> {
                LecturerRegistration lecturer = details.lecturerRegistration();
                userRepository.upsertLecturerRegistration(
                        userId,
                        lecturer.firstName(),
                        lecturer.lastName(),
                        lecturer.title());
            }
            case "COMPANY" -> userRepository.upsertCompanyRegistration(
                    userId,
                    details.companyName(),
                    details.companyWebsite());
            default -> {
            }
        }
    }

    private void ensureUserCanAuthenticate(User user) {
        if (user.getStatus() == AccountStatus.BLOCKED) {
            throw new DomainException("Account has been blocked");
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
                .anyMatch("STUDENT"::equalsIgnoreCase);
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

    private String normalizeExplicitUserType(String userType) {
        if (userType == null || userType.isBlank()) {
            return null;
        }
        return normalizeUserType(userType);
    }

    private GoogleOnboardingDetails resolveGoogleOnboardingDetails(GoogleLoginRequest request, UUID currentUserId) {
        String userType = normalizeExplicitUserType(request == null ? null : request.getUserType());
        if (userType == null) {
            throw new DomainException(GOOGLE_ONBOARDING_REQUIRED);
        }

        return switch (userType) {
            case "STUDENT" -> buildStudentOnboardingDetails(request, currentUserId);
            case "LECTURER" -> buildLecturerOnboardingDetails(request);
            case "COMPANY" -> buildCompanyOnboardingDetails(request);
            default -> throw new DomainException("Unsupported user type: " + userType);
        };
    }

    private GoogleOnboardingDetails buildStudentOnboardingDetails(GoogleLoginRequest request, UUID currentUserId) {
        String fullName = normalizeText(request == null ? null : request.getFullName());
        if (fullName == null) {
            throw new DomainException("Full name is required for student registration");
        }

        String studentCode = validateStudentCode(request == null ? null : request.getStudentId(), true);
        ensureStudentCodeAvailable(studentCode, currentUserId);

        String studentFaculty = normalizeText(request == null ? null : request.getStudentFaculty());
        if (studentFaculty == null) {
            throw new DomainException("Faculty is required for student registration");
        }

        return new GoogleOnboardingDetails("STUDENT", fullName, studentCode, studentFaculty, null, null, null);
    }

    private GoogleOnboardingDetails buildLecturerOnboardingDetails(GoogleLoginRequest request) {
        LecturerRegistration lecturerRegistration = validateLecturerRegistration(
                request == null ? null : request.getFullName(),
                request == null ? null : request.getTitle());
        String fullName = normalizeText(request == null ? null : request.getFullName());
        return new GoogleOnboardingDetails("LECTURER", fullName, null, null, lecturerRegistration, null, null);
    }

    private GoogleOnboardingDetails buildCompanyOnboardingDetails(GoogleLoginRequest request) {
        String representativeName = normalizeText(request == null ? null : request.getFullName());
        if (representativeName == null) {
            throw new DomainException("Representative name is required for company registration");
        }

        String companyName = normalizeText(request == null ? null : request.getCompanyName());
        if (companyName == null) {
            throw new DomainException("Company name is required for company registration");
        }

        String companyWebsite = validateCompanyWebsite(request == null ? null : request.getCompanyWebsite());
        return new GoogleOnboardingDetails("COMPANY", representativeName, null, null, null, companyName, companyWebsite);
    }

    private void ensureStudentCodeAvailable(String studentCode, UUID currentUserId) {
        userRepository.findByStudentCode(studentCode).ifPresent(existingUser -> {
            if (currentUserId == null || !currentUserId.equals(existingUser.getId())) {
                throw new DomainException("Student code already in use");
            }
        });
    }

    private LecturerRegistration validateLecturerRegistration(String fullName, String title) {
        String normalizedFullName = normalizeText(fullName);
        if (normalizedFullName == null) {
            throw new DomainException("Full name is required for lecturer registration");
        }

        String normalizedTitle = normalizeText(title);
        if (normalizedTitle == null) {
            throw new DomainException("Title is required for lecturer registration");
        }

        String[] splitName = splitFullName(normalizedFullName);
        return new LecturerRegistration(splitName[0], splitName[1], normalizedTitle);
    }

    private String validateCompanyRegistration(String companyName, String fullNameFallback) {
        String normalizedCompanyName = normalizeText(companyName);
        if (normalizedCompanyName != null) {
            return normalizedCompanyName;
        }

        String fallback = normalizeText(fullNameFallback);
        if (fallback == null) {
            throw new DomainException("Company name is required for company registration");
        }
        return fallback;
    }

    private String validateCompanyWebsite(String website) {
        String normalized = normalizeText(website);
        if (normalized == null) {
            throw new DomainException("Company website is required for company registration");
        }

        String candidate = normalized.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*$")
                ? normalized
                : "https://" + normalized;
        try {
            URI uri = new URI(candidate);
            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                throw new DomainException("Company website is invalid");
            }
            // Keep the original user input (trimmed) instead of forcing a scheme prefix.
            return normalized;
        } catch (URISyntaxException ex) {
            throw new DomainException("Company website is invalid");
        }
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        return normalized.isEmpty() ? null : normalized;
    }

    private String pictureUrlFromGoogle(GoogleUserInfo googleUser) {
        return googleUser == null ? null : googleUser.pictureUrl();
    }

    private String[] splitFullName(String fullName) {
        String[] parts = fullName.split(" ");
        if (parts.length == 1) {
            return new String[] { parts[0], "" };
        }

        String lastName = parts[parts.length - 1];
        String firstName = String.join(" ", java.util.Arrays.copyOf(parts, parts.length - 1));
        return new String[] { firstName, lastName };
    }

    private void issueEmailVerification(User user) {
        if (user == null || user.getId() == null || user.getEmail() == null) {
            return;
        }

        emailVerificationTokenRepository.deleteExpiredTokens();
        emailVerificationTokenRepository.deleteByUserId(user.getId());

        String token = UUID.randomUUID() + "." + UUID.randomUUID();
        EmailVerificationToken verificationToken = EmailVerificationToken.issue(
                user.getId(),
                token,
                LocalDateTime.now().plusMinutes(emailVerificationTokenTtlMinutes));
        emailVerificationTokenRepository.save(verificationToken);
        emailVerificationNotificationPort.sendVerificationEmail(user.getEmail().value(), token);
    }

    private record LecturerRegistration(String firstName, String lastName, String title) {
    }

    private record GoogleOnboardingDetails(
            String userType,
            String fullName,
            String studentCode,
            String studentFaculty,
            LecturerRegistration lecturerRegistration,
            String companyName,
            String companyWebsite) {
    }
}
