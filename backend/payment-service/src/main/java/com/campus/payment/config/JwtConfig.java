package com.campus.payment.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT utility — validates and parses tokens issued by the User Service.
 * Shares the same secret so the Payment Service can accept User Service JWTs.
 */
@Component
public class JwtConfig {

    @Value("${jwt.secret}")
    private String secret;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Validate a token and return parsed claims.
     */
    public Claims validateToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Extract email (subject) from token.
     */
    public String getEmailFromToken(String token) {
        return validateToken(token).getSubject();
    }

    /**
     * Extract role claim from token.
     */
    public String getRoleFromToken(String token) {
        return validateToken(token).get("role", String.class);
    }

    /**
     * Check if a token is valid (not expired, correct signature).
     */
    public boolean isTokenValid(String token) {
        try {
            validateToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
