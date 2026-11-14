package com.smartbackup.dto.response;

import com.smartbackup.entity.User;

import java.util.UUID;

public record UserSummary(
        UUID id,
        String email,
        String fullName
) {

    public static UserSummary from(User user) {
        return new UserSummary(user.getId(), user.getEmail(), user.getFullName());
    }
}
