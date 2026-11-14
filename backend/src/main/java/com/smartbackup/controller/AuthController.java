package com.smartbackup.controller;

import com.smartbackup.dto.request.LoginRequest;
import com.smartbackup.dto.request.RefreshTokenRequest;
import com.smartbackup.dto.request.RegisterRequest;
import com.smartbackup.dto.response.ApiResponse;
import com.smartbackup.dto.response.AuthResponse;
import com.smartbackup.dto.response.UserResponse;
import com.smartbackup.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentification", description = "Inscription, connexion et gestion des tokens")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Créer un compte utilisateur")
    public ApiResponse<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success(authService.register(request), "Compte créé avec succès");
    }

    @PostMapping("/login")
    @Operation(summary = "Authentifier un utilisateur")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request), "Connexion réussie");
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renouveler la paire de tokens")
    public ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.success(authService.refresh(request.refreshToken()), "Tokens renouvelés");
    }

    @PostMapping("/logout")
    @Operation(summary = "Révoquer le refresh token")
    public ApiResponse<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request.refreshToken());
        return ApiResponse.message("Déconnexion réussie");
    }
}
