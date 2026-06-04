package com.kpiso.api.modules.auth;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService();
    private static final String SECRET_STRING = "mi_clave_secreta_super_segura_y_larga_para_kpiso_api_2026_jwt";
    private static final SecretKey SECRET_KEY = Keys.hmacShaKeyFor(SECRET_STRING.getBytes());

    @Test
    void generateAccessTokenShouldCreateValidToken() {
        String username = "testuser";
        String token = jwtService.generateAccessToken(username);

        assertNotNull(token);

        String subject = Jwts.parser()
                .verifyWith(SECRET_KEY)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();

        assertEquals(username, subject);
    }

    @Test
    void generateRefreshTokenShouldCreateValidToken() {
        String username = "testuser";
        String token = jwtService.generateRefreshToken(username);

        assertNotNull(token);

        String subject = Jwts.parser()
                .verifyWith(SECRET_KEY)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();

        assertEquals(username, subject);
    }
}
