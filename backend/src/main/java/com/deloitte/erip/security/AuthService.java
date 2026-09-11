package com.deloitte.erip.security;

import com.deloitte.erip.audit.AuditService;
import com.deloitte.erip.common.exception.UnauthorizedException;
import com.deloitte.erip.security.dto.AuthResponse;
import com.deloitte.erip.security.dto.LoginRequest;
import com.deloitte.erip.user.User;
import com.deloitte.erip.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final AuditService auditService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${erip.jwt.access-token-ttl-minutes}")
    private long accessTokenTtlMinutes;

    @Value("${erip.jwt.refresh-token-ttl-days}")
    private long refreshTokenTtlDays;

    @Transactional
    public AuthResponse login(LoginRequest request) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        } catch (Exception ex) {
            auditService.record("LOGIN_FAILED", "USER", request.email(), Map.of("reason", "bad_credentials"));
            throw ex;
        }

        User user = (User) authentication.getPrincipal();
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        String accessToken = jwtService.generateAccessToken(user, user.getRole().name(), user.getId().toString());
        String rawRefreshToken = issueRefreshToken(user);

        auditService.record("LOGIN_SUCCESS", "USER", user.getId().toString(), Map.of("email", user.getEmail()));

        return toAuthResponse(user, accessToken, rawRefreshToken);
    }

    @Transactional
    public AuthResponse refresh(String rawRefreshToken) {
        String hash = hash(rawRefreshToken);
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (stored.isRevoked() || stored.getExpiresAt().isBefore(Instant.now())) {
            throw new UnauthorizedException("Refresh token expired or revoked");
        }

        User user = stored.getUser();
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        String accessToken = jwtService.generateAccessToken(user, user.getRole().name(), user.getId().toString());
        String newRawRefreshToken = issueRefreshToken(user);

        return toAuthResponse(user, accessToken, newRawRefreshToken);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenRepository.findByTokenHash(hash(rawRefreshToken))
                .ifPresent(token -> {
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);
                });
    }

    private String issueRefreshToken(User user) {
        byte[] bytes = new byte[64];
        secureRandom.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(hash(rawToken))
                .expiresAt(Instant.now().plus(refreshTokenTtlDays, ChronoUnit.DAYS))
                .revoked(false)
                .createdAt(Instant.now())
                .build();
        refreshTokenRepository.save(refreshToken);
        return rawToken;
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.getBytes());
            return Base64.getEncoder().encodeToString(hashed);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to hash token", ex);
        }
    }

    private AuthResponse toAuthResponse(User user, String accessToken, String refreshToken) {
        return new AuthResponse(
                accessToken,
                refreshToken,
                "Bearer",
                accessTokenTtlMinutes * 60,
                new AuthResponse.UserSummary(
                        user.getId().toString(),
                        user.getEmail(),
                        user.getFullName(),
                        user.getRole().name(),
                        user.getBusinessUnit()));
    }
}
