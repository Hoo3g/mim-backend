package com.hus.mim_backend.application.auth.service;

import com.hus.mim_backend.application.auth.usecase.VerifiedAccountUseCase;
import com.hus.mim_backend.application.port.output.UserRepository;
import com.hus.mim_backend.domain.auth.model.AccountStatus;
import com.hus.mim_backend.domain.auth.model.Email;
import com.hus.mim_backend.domain.auth.model.User;
import com.hus.mim_backend.domain.shared.AuthException;
import com.hus.mim_backend.domain.shared.DomainException;
import org.springframework.util.StringUtils;

import java.util.UUID;

/**
 * Shared application service for resolving authenticated user identity.
 */
public class VerifiedAccountServiceImpl implements VerifiedAccountUseCase {
    private static final String UNVERIFIED_MESSAGE_NEWS = "Email chưa được xác thực. Tài khoản chỉ được xem nội dung cho tới khi hoàn tất xác thực email.";
    private static final String UNVERIFIED_MESSAGE_STORAGE = "Email chưa được xác thực. Tài khoản hiện chỉ được phép xem nội dung cho tới khi hoàn tất xác thực email.";

    private final UserRepository userRepository;

    public VerifiedAccountServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UUID requireVerifiedUserId(String email) {
        User user = resolveVerifiedUser(normalizeEmail(email), UNVERIFIED_MESSAGE_NEWS);
        return user.getId();
    }

    @Override
    public String requireVerifiedEmail(String email) {
        String normalizedEmail = normalizeEmail(email);
        resolveVerifiedUser(normalizedEmail, UNVERIFIED_MESSAGE_STORAGE);
        return normalizedEmail;
    }

    private User resolveVerifiedUser(String normalizedEmail, String notVerifiedMessage) {
        User user = userRepository.findByEmail(new Email(normalizedEmail))
                .orElseThrow(() -> new DomainException("Authenticated user is not found"));
        if (user.getStatus() != AccountStatus.APPROVED) {
            throw new DomainException(notVerifiedMessage);
        }
        return user;
    }

    private String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            throw new AuthException("Authentication required");
        }
        return email.trim();
    }
}
