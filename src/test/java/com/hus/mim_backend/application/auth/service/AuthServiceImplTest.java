package com.hus.mim_backend.application.auth.service;

import com.hus.mim_backend.application.auth.dto.GoogleLoginRequest;
import com.hus.mim_backend.application.auth.dto.RegisterRequest;
import com.hus.mim_backend.application.auth.model.GoogleUserInfo;
import com.hus.mim_backend.application.port.output.EmailVerificationNotificationPort;
import com.hus.mim_backend.application.port.output.EmailVerificationTokenRepository;
import com.hus.mim_backend.application.port.output.GoogleTokenVerifier;
import com.hus.mim_backend.application.port.output.PasswordEncoder;
import com.hus.mim_backend.application.port.output.RefreshTokenRepository;
import com.hus.mim_backend.application.port.output.TokenProvider;
import com.hus.mim_backend.application.port.output.UserRepository;
import com.hus.mim_backend.domain.auth.model.Email;
import com.hus.mim_backend.domain.auth.model.EmailVerificationToken;
import com.hus.mim_backend.domain.auth.model.RefreshToken;
import com.hus.mim_backend.domain.auth.model.User;
import com.hus.mim_backend.domain.shared.DomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthServiceImplTest {

    private FakeUserRepository userRepository;
    private FakeEmailVerificationTokenRepository emailVerificationTokenRepository;
    private FakeEmailVerificationNotificationPort emailVerificationNotificationPort;
    private FakeGoogleTokenVerifier googleTokenVerifier;
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        userRepository = new FakeUserRepository();
        emailVerificationTokenRepository = new FakeEmailVerificationTokenRepository();
        emailVerificationNotificationPort = new FakeEmailVerificationNotificationPort();
        googleTokenVerifier = new FakeGoogleTokenVerifier();

        authService = new AuthServiceImpl(
                userRepository,
                new FakePasswordEncoder(),
                new FakeTokenProvider(),
                new FakeRefreshTokenRepository(),
                googleTokenVerifier,
                emailVerificationTokenRepository,
                emailVerificationNotificationPort,
                60
        );
    }

    @Test
    void registerLecturerShouldSaveLecturerProfile() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("lecturer@hus.edu.vn");
        request.setPassword("secret123");
        request.setFullName("Nguyen Van A");
        request.setTitle("Giảng viên");
        request.setUserType("LECTURER");

        authService.register(request);

        assertNotNull(userRepository.savedUser);
        assertEquals(Set.of("LECTURER"), userRepository.savedUser.getRoles());
        assertEquals(userRepository.savedUser.getId(), userRepository.lecturerRegistrationUserId);
        assertEquals("Nguyen Van", userRepository.lecturerFirstName);
        assertEquals("A", userRepository.lecturerLastName);
        assertEquals("Giảng viên", userRepository.lecturerTitle);
        assertEquals("lecturer@hus.edu.vn", emailVerificationNotificationPort.recipientEmail);
        assertNotNull(emailVerificationNotificationPort.token);
        assertTrue(emailVerificationNotificationPort.token.contains("."));
        assertNotNull(emailVerificationTokenRepository.savedToken);
    }

    @Test
    void registerLecturerShouldRequireTitle() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("lecturer@hus.edu.vn");
        request.setPassword("secret123");
        request.setFullName("Nguyen Van A");
        request.setTitle(" ");
        request.setUserType("LECTURER");

        assertThrows(DomainException.class, () -> authService.register(request));
        assertEquals(null, userRepository.savedUser);
        assertEquals(null, userRepository.lecturerRegistrationUserId);
    }

    @Test
    void registerCompanyShouldSaveCompanyProfile() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("hr@company.com");
        request.setPassword("secret123");
        request.setFullName("Cong ty ABC");
        request.setCompanyName("Công ty ABC");
        request.setUserType("COMPANY");

        authService.register(request);

        assertNotNull(userRepository.savedUser);
        assertEquals(Set.of("COMPANY"), userRepository.savedUser.getRoles());
        assertEquals(userRepository.savedUser.getId(), userRepository.companyRegistrationUserId);
        assertEquals("Công ty ABC", userRepository.companyRegistrationName);
    }

    @Test
    void googleLoginShouldRequireOnboardingForNewAccount() {
        googleTokenVerifier.userInfo = new GoogleUserInfo("google@hus.edu.vn", "Nguyen Van A", "avatar", true);

        GoogleLoginRequest request = new GoogleLoginRequest();
        request.setIdToken("token");

        DomainException ex = assertThrows(DomainException.class, () -> authService.loginWithGoogle(request));
        assertEquals("GOOGLE_ONBOARDING_REQUIRED", ex.getMessage());
    }

    @Test
    void googleLoginShouldCreateUserAfterOnboarding() {
        googleTokenVerifier.userInfo = new GoogleUserInfo("google@hus.edu.vn", "Nguyen Van A", "avatar", true);

        GoogleLoginRequest request = new GoogleLoginRequest();
        request.setIdToken("token");
        request.setUserType("LECTURER");
        request.setFullName("Nguyen Van A");
        request.setTitle("TS.");

        authService.loginWithGoogle(request);

        assertNotNull(userRepository.savedUser);
        assertEquals("Nguyen Van A", userRepository.savedUser.getFullName());
        assertEquals(Set.of("LECTURER"), userRepository.savedUser.getRoles());
        assertEquals(userRepository.savedUser.getId(), userRepository.lecturerRegistrationUserId);
        assertEquals("Nguyen Van", userRepository.lecturerFirstName);
        assertEquals("A", userRepository.lecturerLastName);
        assertEquals("TS.", userRepository.lecturerTitle);
    }

    @Test
    void googleLoginShouldRequireOnboardingForExistingIncompleteAccount() {
        User existingUser = User.createNew(new Email("google@hus.edu.vn"), "encoded", "STUDENT", com.hus.mim_backend.domain.auth.model.AccountStatus.APPROVED);
        userRepository.savedUser = existingUser;
        googleTokenVerifier.userInfo = new GoogleUserInfo("google@hus.edu.vn", "Nguyen Van A", "avatar", true);

        GoogleLoginRequest request = new GoogleLoginRequest();
        request.setIdToken("token");

        DomainException ex = assertThrows(DomainException.class, () -> authService.loginWithGoogle(request));
        assertEquals("GOOGLE_ONBOARDING_REQUIRED", ex.getMessage());
    }

    @Test
    void googleLoginShouldCompleteExistingIncompleteAccountAfterOnboarding() {
        User existingUser = User.createNew(new Email("google@hus.edu.vn"), "encoded", "STUDENT", com.hus.mim_backend.domain.auth.model.AccountStatus.APPROVED);
        userRepository.savedUser = existingUser;
        googleTokenVerifier.userInfo = new GoogleUserInfo("google@hus.edu.vn", "Nguyen Van A", "avatar", true);

        GoogleLoginRequest request = new GoogleLoginRequest();
        request.setIdToken("token");
        request.setUserType("STUDENT");
        request.setFullName("Nguyen Van A");
        request.setStudentId("21001234");
        request.setStudentFaculty("Khoa Toan - Co - Tin hoc");

        authService.loginWithGoogle(request);

        assertEquals(existingUser.getId(), userRepository.studentRegistrationUserId);
        assertEquals("Nguyen Van", userRepository.studentFirstName);
        assertEquals("A", userRepository.studentLastName);
        assertEquals("21001234", userRepository.studentCode);
        assertEquals("Khoa Toan - Co - Tin hoc", userRepository.studentMajor);
        assertTrue(userRepository.studentRegistrationExists);
    }

    @Test
    void googleLoginShouldCreateCompanyProfileAfterOnboarding() {
        googleTokenVerifier.userInfo = new GoogleUserInfo("company@hus.edu.vn", "Nguyen Van B", "avatar", true);

        GoogleLoginRequest request = new GoogleLoginRequest();
        request.setIdToken("token");
        request.setUserType("COMPANY");
        request.setFullName("Nguyen Van B");
        request.setCompanyName("Cong ty ABC");
        request.setCompanyWebsite("company.example.com");

        authService.loginWithGoogle(request);

        assertNotNull(userRepository.savedUser);
        assertEquals(Set.of("COMPANY"), userRepository.savedUser.getRoles());
        assertEquals(userRepository.savedUser.getId(), userRepository.companyRegistrationUserId);
        assertEquals("Cong ty ABC", userRepository.companyRegistrationName);
        assertEquals("company.example.com", userRepository.companyRegistrationWebsite);
    }

    private static final class FakeUserRepository implements UserRepository {
        private User savedUser;
        private UUID studentRegistrationUserId;
        private String studentFirstName;
        private String studentLastName;
        private String studentMajor;
        private String studentCode;
        private boolean studentRegistrationExists;
        private UUID lecturerRegistrationUserId;
        private boolean lecturerRegistrationExists;
        private String lecturerFirstName;
        private String lecturerLastName;
        private String lecturerTitle;
        private UUID companyRegistrationUserId;
        private String companyRegistrationName;
        private String companyRegistrationWebsite;
        private boolean companyRegistrationExists;

        @Override
        public Optional<User> findById(UUID id) {
            return Optional.empty();
        }

        @Override
        public Optional<User> findByEmail(Email email) {
            if (savedUser != null && savedUser.getEmail() != null
                    && savedUser.getEmail().value().equalsIgnoreCase(email.value())) {
                return Optional.of(savedUser);
            }
            return Optional.empty();
        }

        @Override
        public Optional<UUID> findIdByEmail(String email) {
            return Optional.empty();
        }

        @Override
        public Optional<User> findByStudentCode(String studentCode) {
            return Optional.empty();
        }

        @Override
        public User save(User user) {
            this.savedUser = user;
            return user;
        }

        @Override
        public boolean existsByEmail(Email email) {
            return false;
        }

        @Override
        public boolean existsByStudentCode(String studentCode) {
            return false;
        }

        @Override
        public boolean hasStudentRegistration(UUID userId) {
            return studentRegistrationExists && userId != null && userId.equals(studentRegistrationUserId);
        }

        @Override
        public boolean hasLecturerRegistration(UUID userId) {
            return lecturerRegistrationExists && userId != null && userId.equals(lecturerRegistrationUserId);
        }

        @Override
        public boolean hasCompanyRegistration(UUID userId) {
            return companyRegistrationExists && userId != null && userId.equals(companyRegistrationUserId);
        }

        @Override
        public void upsertStudentRegistration(UUID userId, String firstName, String lastName, String major) {
            this.studentRegistrationUserId = userId;
            this.studentFirstName = firstName;
            this.studentLastName = lastName;
            this.studentMajor = major;
            this.studentRegistrationExists = true;
        }

        @Override
        public void upsertStudentCode(UUID userId, String studentCode) {
            this.studentCode = studentCode;
        }

        @Override
        public void upsertLecturerRegistration(UUID userId, String firstName, String lastName, String title) {
            this.lecturerRegistrationUserId = userId;
            this.lecturerFirstName = firstName;
            this.lecturerLastName = lastName;
            this.lecturerTitle = title;
            this.lecturerRegistrationExists = true;
        }

        @Override
        public void upsertCompanyRegistration(UUID userId, String companyName, String website) {
            this.companyRegistrationUserId = userId;
            this.companyRegistrationName = companyName;
            this.companyRegistrationWebsite = website;
            this.companyRegistrationExists = true;
        }

        @Override
        public List<User> findByAccountStatus(String status) {
            return List.of();
        }

        @Override
        public void deleteById(UUID id) {
        }
    }

    private static final class FakePasswordEncoder implements PasswordEncoder {
        @Override
        public String encode(String rawPassword) {
            return "encoded:" + rawPassword;
        }

        @Override
        public boolean matches(String rawPassword, String encodedPassword) {
            return encodedPassword.equals(encode(rawPassword));
        }
    }

    private static final class FakeTokenProvider implements TokenProvider {
        @Override
        public String generateAccessToken(User user) {
            return "access";
        }

        @Override
        public String generateRefreshToken(User user) {
            return "refresh";
        }

        @Override
        public boolean validateToken(String token) {
            return true;
        }

        @Override
        public String getEmailFromToken(String token) {
            return "";
        }

        @Override
        public Set<String> getRolesFromToken(String token) {
            return Set.of();
        }

        @Override
        public LocalDateTime getExpiryFromToken(String token) {
            return LocalDateTime.now().plusDays(7);
        }
    }

    private static final class FakeRefreshTokenRepository implements RefreshTokenRepository {
        @Override
        public Optional<RefreshToken> findByToken(String token) {
            return Optional.empty();
        }

        @Override
        public Optional<RefreshToken> findByUserId(UUID userId) {
            return Optional.empty();
        }

        @Override
        public RefreshToken save(RefreshToken refreshToken) {
            return refreshToken;
        }

        @Override
        public void revokeByUserId(UUID userId) {
        }

        @Override
        public void deleteExpiredTokens() {
        }
    }

    private static final class FakeGoogleTokenVerifier implements GoogleTokenVerifier {
        private GoogleUserInfo userInfo;

        @Override
        public GoogleUserInfo verifyIdToken(String idToken) {
            if (userInfo == null) {
                throw new UnsupportedOperationException();
            }
            return userInfo;
        }
    }

    private static final class FakeEmailVerificationTokenRepository implements EmailVerificationTokenRepository {
        private EmailVerificationToken savedToken;

        @Override
        public Optional<EmailVerificationToken> findByToken(String token) {
            return Optional.empty();
        }

        @Override
        public EmailVerificationToken save(EmailVerificationToken token) {
            this.savedToken = token;
            return token;
        }

        @Override
        public void deleteByUserId(UUID userId) {
        }

        @Override
        public void deleteByToken(String token) {
        }

        @Override
        public void deleteExpiredTokens() {
        }
    }

    private static final class FakeEmailVerificationNotificationPort implements EmailVerificationNotificationPort {
        private String recipientEmail;
        private String token;

        @Override
        public void sendVerificationEmail(String recipientEmail, String token) {
            this.recipientEmail = recipientEmail;
            this.token = token;
        }
    }
}
