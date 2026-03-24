package com.hus.mim_backend.infrastructure.adapter.security;

import com.hus.mim_backend.domain.auth.model.AccountStatus;
import com.hus.mim_backend.domain.auth.model.Email;
import com.hus.mim_backend.domain.auth.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtTokenAdapterTest {

    @Test
    void generateRefreshTokenShouldBeUniqueAcrossBackToBackCalls() {
        JwtTokenAdapter tokenAdapter = new JwtTokenAdapter();
        ReflectionTestUtils.setField(tokenAdapter, "jwtSecret",
                "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");
        ReflectionTestUtils.setField(tokenAdapter, "refreshTokenExpiration", 86_400_000L);
        ReflectionTestUtils.setField(tokenAdapter, "accessTokenExpiration", 3_600_000L);

        User user = User.builder()
                .id(UUID.randomUUID())
                .email(new Email("token-test@example.com"))
                .password("noop")
                .status(AccountStatus.APPROVED)
                .roles(Set.of("STUDENT"))
                .build();

        String first = tokenAdapter.generateRefreshToken(user);
        String second = tokenAdapter.generateRefreshToken(user);

        assertNotEquals(first, second);
        assertTrue(tokenAdapter.validateToken(first));
        assertTrue(tokenAdapter.validateToken(second));
    }
}
