package com.smartbackup.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "Le mot de passe actuel est requis")
        String currentPassword,

        @NotBlank(message = "Le nouveau mot de passe est requis")
        @Size(min = 8, max = 100, message = "Le nouveau mot de passe doit contenir entre 8 et 100 caractères")
        @Pattern(
                regexp = "^(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$",
                message = "Le nouveau mot de passe doit contenir au moins une majuscule, un chiffre et un caractère spécial"
        )
        String newPassword,

        @NotBlank(message = "La confirmation du mot de passe est requise")
        String confirmPassword
) {
}
