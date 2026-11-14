package com.smartbackup.security;

import com.smartbackup.entity.User;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private static final String SECRET = "test-secret-key-with-at-least-32-characters-for-hs256";

    private final JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(SECRET, 3_600_000L);

    @Test
    void generatesTokenWithUserClaims() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("alice@example.com")
                .build();

        String token = jwtTokenProvider.generateAccessToken(user);

        assertThat(jwtTokenProvider.isValid(token)).isTrue();
        assertThat(jwtTokenProvider.getUserId(token)).isEqualTo(user.getId());
        assertThat(jwtTokenProvider.getEmail(token)).isEqualTo("alice@example.com");
    }

    @Test
    void rejectsMalformedToken() {
        assertThat(jwtTokenProvider.isValid("not-a-jwt")).isFalse();
    }

    @Test
    void rejectsTokenSignedWithAnotherKey() {
        JwtTokenProvider otherProvider = new JwtTokenProvider(
                "another-secret-key-with-at-least-32-characters-for-hs256", 3_600_000L);
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("bob@example.com")
                .build();

        String token = otherProvider.generateAccessToken(user);

        assertThat(jwtTokenProvider.isValid(token)).isFalse();
    }
}
