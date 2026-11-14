package com.smartbackup.service;

import com.smartbackup.dto.request.LoginRequest;
import com.smartbackup.dto.request.RegisterRequest;
import com.smartbackup.dto.response.AuthResponse;
import com.smartbackup.dto.response.UserResponse;
import com.smartbackup.dto.response.UserSummary;
import com.smartbackup.entity.User;
import com.smartbackup.exception.EmailAlreadyUsedException;
import com.smartbackup.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new EmailAlreadyUsedException("Un compte avec cet email existe déjà");
        }

        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(request.password()))
                .fullName(request.fullName().trim())
                .active(true)
                .build();

        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(normalizeEmail(request.email()))
                .orElseThrow(() -> new BadCredentialsException("Identifiants incorrects"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Identifiants incorrects");
        }

        if (!user.isActive()) {
            throw new DisabledException("Compte désactivé");
        }

        return toAuthResponse(tokenService.issueTokens(user));
    }

    @Transactional
    public AuthResponse refresh(String refreshToken) {
        return toAuthResponse(tokenService.rotateTokens(refreshToken));
    }

    @Transactional
    public void logout(String refreshToken) {
        tokenService.revoke(refreshToken);
    }

    private AuthResponse toAuthResponse(TokenService.IssuedTokens tokens) {
        return new AuthResponse(
                tokens.accessToken(),
                "Bearer",
                tokens.expiresIn(),
                tokens.refreshToken(),
                UserSummary.from(tokens.user())
        );
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
