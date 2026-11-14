package com.smartbackup.controller;

import com.smartbackup.dto.request.ChangePasswordRequest;
import com.smartbackup.dto.request.UpdateProfileRequest;
import com.smartbackup.dto.response.ApiResponse;
import com.smartbackup.dto.response.UserResponse;
import com.smartbackup.security.UserPrincipal;
import com.smartbackup.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Utilisateurs", description = "Profil de l'utilisateur authentifié")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "Récupérer le profil connecté")
    public ApiResponse<UserResponse> getProfile(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(userService.getProfile(principal.getId()), null);
    }

    @PutMapping("/me")
    @Operation(summary = "Mettre à jour le profil connecté")
    public ApiResponse<UserResponse> updateProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        return ApiResponse.success(userService.updateProfile(principal.getId(), request), "Profil mis à jour");
    }

    @PutMapping("/me/password")
    @Operation(summary = "Changer le mot de passe")
    public ApiResponse<Void> changePassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        userService.changePassword(principal, request);
        return ApiResponse.message("Mot de passe modifié avec succès");
    }
}
