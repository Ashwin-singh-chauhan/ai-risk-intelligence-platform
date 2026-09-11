package com.deloitte.erip.security.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresInSeconds,
        UserSummary user
) {
    public record UserSummary(String id, String email, String fullName, String role, String businessUnit) {
    }
}
