package com.smartbackup.service;

import com.smartbackup.dto.request.ChangePasswordRequest;
import com.smartbackup.dto.request.UpdateProfileRequest;
import com.smartbackup.dto.response.UserResponse;
import com.smartbackup.entity.User;
import com.smartbackup.exception.BadRequestException;
import com.smartbackup.exception.ResourceNotFoundException;
import com.smartbackup.repository.UserRepository;
import com.smartbackup.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    @Transactional(readOnly = true)
    public UserResponse getProfile(UUID userId) {
        return UserResponse.from(findUser(userId));
    }

    @Transactional
    public UserResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = findUser(userId);
        user.setFullName(request.fullName().trim());
        return UserResponse.from(user);
    }

    @Transactional
    public void changePassword(UserPrincipal principal, ChangePasswordRequest request) {
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new BadRequestException("La confirmation du mot de passe ne correspond pas");
        }

        User user = findUser(principal.getId());

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Le mot de passe actuel est incorrect");
        }

        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Le nouveau mot de passe doit être différent de l'ancien");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        tokenService.revokeAllForUser(user.getId());
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
    }
}
