package com.inventory.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import org.springframework.core.env.Environment;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
public class JwtService {

    private static final int MIN_UNIQUE_CHARS = 10;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    private final Environment environment;

    public JwtService(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void validateSecret() {
        if (jwtSecret == null || jwtSecret.isBlank() || jwtSecret.length() < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET is not set or too short (minimum 32 characters). Application cannot start.");
        }
        long uniqueChars = jwtSecret.chars().distinct().count();
        if (uniqueChars < MIN_UNIQUE_CHARS) {
            throw new IllegalStateException(
                    "JWT_SECRET has insufficient entropy (too few unique characters). Generate with: openssl rand -base64 32");
        }
        boolean isProd = Arrays.asList(environment.getActiveProfiles()).contains("prod");
        if (isProd && jwtSecret.startsWith("dev-only-")) {
            throw new IllegalStateException(
                    "Dev JWT secret detected in production! Set a unique JWT_SECRET environment variable.");
        }
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(UUID userId, String email, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
            .subject(userId.toString())
            .claim("email", email)
            .claim("role", role)
            .issuedAt(now)
            .expiration(expiry)
            .signWith(getSigningKey())
            .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (ExpiredJwtException e) {
            return false;
        } catch (SecurityException | MalformedJwtException e) {
            log.warn("Invalid JWT signature/format — possible attack: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Unexpected error validating JWT", e);
            return false;
        }
    }

}
