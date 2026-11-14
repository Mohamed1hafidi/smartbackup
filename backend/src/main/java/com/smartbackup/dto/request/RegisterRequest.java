package com.smartbackup.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "L'email est requis")
        @Email(message = "Format d'email invalide")
        @Size(max = 320, message = "L'email ne doit pas dépasser 320 caractères")
        String email,

        @NotBlank(message = "Le mot de passe est requis")
        @Size(min = 8, max = 100, message = "Le mot de passe doit contenir entre 8 et 100 caractères")
        @Pattern(
                regexp = "^(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$",
                message = "Le mot de passe doit contenir au moins une majuscule, un chiffre et un caractère spécial"
        )
        String password,

        @NotBlank(message = "Le nom complet est requis")
        @Size(min = 2, max = 100, message = "Le nom complet doit contenir entre 2 et 100 caractères")
        String fullName
) {
}
