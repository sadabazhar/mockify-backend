package com.mockify.backend.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

@Component
@Slf4j
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    // Generate signing key from secret
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // Generate JWT token for a user
    public String generateToken(Long userId) {
        Date now = new Date();
        Date expirationTime = new Date(now.getTime() + jwtExpiration);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuer("mockify-api")
                .audience().add("mockify-web").and()
                .issuedAt(now)
                .expiration(expirationTime)
                .notBefore(now)
                .id(UUID.randomUUID().toString())
                .claim("type", "access")
                .signWith(getSigningKey())
                .compact();
    }

    // Parses and returns all claims from a token.
    public Claims getAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // Extract user ID from token
    public Long getUserIdFromToken(String token) {
        try {
            Claims claims = getAllClaims(token);
            return Long.parseLong(claims.getSubject());
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            return null;
        }
    }

    // Core Validation method
    // JJWT parser automatically validates: signature, expiration, notBefore
    public boolean validateToken(String token) {
        try {
            Claims claims = getAllClaims(token);

            // Check token type for access token
            String type = claims.get("type", String.class);
            if (!"access".equals(type)) {
                log.warn("Invalid token type: {}", type);
                return false;
            }

            // Check issuer
            if (!"mockify-api".equals(claims.getIssuer())) return false;

            // Check audience
            if (claims.getAudience() == null || !claims.getAudience().contains("mockify-web")) return false;

            return true;

        } catch (ExpiredJwtException e) {
            log.debug("JWT token is expired: {}", e.getMessage());
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Invalid JWT: {}", e.getMessage());
        }
        return false;
    }
}
