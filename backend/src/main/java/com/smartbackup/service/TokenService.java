package com.smartbackup.service;

import com.smartbackup.entity.RefreshToken;
import com.smartbackup.entity.User;
import com.smartbackup.exception.InvalidTokenException;
import com.smartbackup.repository.RefreshTokenRepository;
import com.smartbackup.security.JwtTokenProvider;
import com.smartbackup.util.HashUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class TokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final long refreshExpirationMs;

    public TokenService(
            RefreshTokenRepository refreshTokenRepository,
            JwtTokenProvider jwtTokenProvider,
            @Value("${jwt.refresh-expiration-ms}") long refreshExpirationMs
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    @Transactional
    public IssuedTokens issueTokens(User user) {
        return buildTokens(user);
    }

    @Transactional
    public IssuedTokens rotateTokens(String rawRefreshToken) {
        String tokenHash = HashUtil.sha256(rawRefreshToken);
        RefreshToken stored = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidTokenException("Refresh token invalide ou expiré"));

        if (stored.isRevoked() || stored.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidTokenException("Refresh token invalide ou expiré");
        }

        stored.setRevoked(true);
        return buildTokens(stored.getUser());
    }

    @Transactional
    public void revoke(String rawRefreshToken) {
        refreshTokenRepository.findByTokenHash(HashUtil.sha256(rawRefreshToken))
                .filter(token -> !token.isRevoked())
                .ifPresent(token -> token.setRevoked(true));
    }

    @Transactional
    public void revokeAllForUser(UUID userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
    }

    private IssuedTokens buildTokens(User user) {
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = HashUtil.generateRefreshToken();

        refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .tokenHash(HashUtil.sha256(refreshToken))
                .expiresAt(Instant.now().plusMillis(refreshExpirationMs))
                .revoked(false)
                .build());

        return new IssuedTokens(
                user,
                accessToken,
                refreshToken,
                jwtTokenProvider.getExpirationMs() / 1000
        );
    }

    public record IssuedTokens(User user, String accessToken, String refreshToken, long expiresIn) {
    }
}
