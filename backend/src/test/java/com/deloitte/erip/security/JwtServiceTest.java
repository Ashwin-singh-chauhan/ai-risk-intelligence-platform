package com.deloitte.erip.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(
                "test-secret-key-for-unit-tests-only-must-be-long-enough-for-hs256",
                30,
                "erip-platform-test");
    }

    private UserDetails userDetails(String email) {
        return new User(email, "irrelevant", List.of());
    }

    @Test
    void generatedToken_roundTripsUsernameAndRole() {
        UserDetails user = userDetails("analyst@erip.com");
        String token = jwtService.generateAccessToken(user, "SECURITY_ANALYST", "user-id-123");

        assertThat(jwtService.extractUsername(token)).isEqualTo("analyst@erip.com");
        assertThat(jwtService.extractRole(token)).isEqualTo("SECURITY_ANALYST");
    }

    @Test
    void isTokenValid_isTrueForMatchingUserAndUnexpiredToken() {
        UserDetails user = userDetails("viewer@erip.com");
        String token = jwtService.generateAccessToken(user, "VIEWER", "user-id-456");

        assertThat(jwtService.isTokenValid(token, user)).isTrue();
    }

    @Test
    void isTokenValid_isFalseForDifferentUser() {
        UserDetails issuedFor = userDetails("admin@erip.com");
        UserDetails otherUser = userDetails("someone-else@erip.com");
        String token = jwtService.generateAccessToken(issuedFor, "ADMIN", "user-id-789");

        assertThat(jwtService.isTokenValid(token, otherUser)).isFalse();
    }
}
