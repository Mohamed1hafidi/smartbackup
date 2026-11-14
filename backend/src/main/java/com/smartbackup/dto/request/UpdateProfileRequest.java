package com.smartbackup.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank(message = "Le nom complet est requis")
        @Size(min = 2, max = 100, message = "Le nom complet doit contenir entre 2 et 100 caractères")
        String fullName
) {
}
