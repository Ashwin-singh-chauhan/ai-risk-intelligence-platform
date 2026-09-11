package com.deloitte.erip.user.dto;

import com.deloitte.erip.user.Role;
import com.deloitte.erip.user.User;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String fullName,
        Role role,
        String businessUnit,
        boolean active,
        Instant lastLoginAt,
        Instant createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(), user.getEmail(), user.getFullName(), user.getRole(),
                user.getBusinessUnit(), user.isActive(), user.getLastLoginAt(), user.getCreatedAt());
    }
}
