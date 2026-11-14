package com.smartbackup.service;

import com.smartbackup.dto.request.LoginRequest;
import com.smartbackup.dto.request.RegisterRequest;
import com.smartbackup.dto.response.AuthResponse;
import com.smartbackup.dto.response.UserResponse;
import com.smartbackup.entity.User;
import com.smartbackup.exception.EmailAlreadyUsedException;
import com.smartbackup.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenService tokenService;

    @InjectMocks
    private AuthService authService;

    @Test
    void registersUserWithNormalizedEmailAndHashedPassword() {
        when(userRepository.existsByEmailIgnoreCase("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Secure@123")).thenReturn("bcrypt-hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = authService.register(
                new RegisterRequest("  Alice@Example.com ", "Secure@123", "Alice Martin"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();

        assertThat(saved.getEmail()).isEqualTo("alice@example.com");
        assertThat(saved.getPasswordHash()).isEqualTo("bcrypt-hash");
        assertThat(saved.getFullName()).isEqualTo("Alice Martin");
        assertThat(saved.isActive()).isTrue();
        assertThat(response.email()).isEqualTo("alice@example.com");
    }

    @Test
    void rejectsDuplicateEmail() {
        when(userRepository.existsByEmailIgnoreCase("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("alice@example.com", "Secure@123", "Alice Martin")))
                .isInstanceOf(EmailAlreadyUsedException.class);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void logsInAndReturnsTokens() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("alice@example.com")
                .passwordHash("bcrypt-hash")
                .fullName("Alice Martin")
                .active(true)
                .build();

        when(userRepository.findByEmailIgnoreCase("alice@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Secure@123", "bcrypt-hash")).thenReturn(true);
        when(tokenService.issueTokens(user))
                .thenReturn(new TokenService.IssuedTokens(user, "access-token", "refresh-token", 86_400L));

        AuthResponse response = authService.login(new LoginRequest("alice@example.com", "Secure@123"));

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(86_400L);
        assertThat(response.user().id()).isEqualTo(user.getId());
    }

    @Test
    void rejectsInvalidPassword() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("alice@example.com")
                .passwordHash("bcrypt-hash")
                .active(true)
                .build();

        when(userRepository.findByEmailIgnoreCase("alice@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Wrong@123", "bcrypt-hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("alice@example.com", "Wrong@123")))
                .isInstanceOf(BadCredentialsException.class);

        verify(tokenService, never()).issueTokens(any(User.class));
    }

    @Test
    void rejectsDisabledAccount() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("alice@example.com")
                .passwordHash("bcrypt-hash")
                .active(false)
                .build();

        when(userRepository.findByEmailIgnoreCase("alice@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Secure@123", "bcrypt-hash")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(new LoginRequest("alice@example.com", "Secure@123")))
                .isInstanceOf(DisabledException.class);
    }
}
