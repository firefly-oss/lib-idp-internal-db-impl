/*
 * Copyright 2025 Firefly Software Solutions Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.firefly.idp.internaldb.service;

import com.firefly.idp.internaldb.config.InternalDbProperties;
import com.firefly.idp.internaldb.domain.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for JWT token generation and validation.
 */
@Service
@Slf4j
public class JwtTokenService {

    private final InternalDbProperties properties;
    private final SecretKey secretKey;

    public JwtTokenService(InternalDbProperties properties) {
        this.properties = properties;
        // Create a secure key from the configured secret
        this.secretKey = Keys.hmacShaKeyFor(properties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generate an access token for a user.
     *
     * @param user the user
     * @param roles the user's roles
     * @return the JWT access token
     */
    public String generateAccessToken(User user, List<String> roles) {
        Instant now = Instant.now();
        Instant expiration = now.plus(properties.getJwt().getAccessTokenExpiration(), ChronoUnit.MILLIS);

        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("username", user.getUsername())
                .claim("email", user.getEmail())
                .claim("firstName", user.getFirstName())
                .claim("lastName", user.getLastName())
                .claim("roles", roles)
                .claim("type", "access")
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .issuer(properties.getJwt().getIssuer())
                .signWith(secretKey)
                .compact();
    }

    /**
     * Generate a refresh token for a user.
     *
     * @param user the user
     * @param sessionId the session ID
     * @return the JWT refresh token
     */
    public String generateRefreshToken(User user, UUID sessionId) {
        Instant now = Instant.now();
        Instant expiration = now.plus(properties.getJwt().getRefreshTokenExpiration(), ChronoUnit.MILLIS);

        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("type", "refresh")
                .claim("sessionId", sessionId.toString())
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .issuer(properties.getJwt().getIssuer())
                .signWith(secretKey)
                .compact();
    }

    /**
     * Parse and validate a JWT token.
     *
     * @param token the JWT token
     * @return the parsed claims
     * @throws JwtException if the token is invalid
     */
    public Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException e) {
            log.error("Failed to parse JWT token", e);
            throw e;
        }
    }

    /**
     * Extract the JTI (JWT ID) from a token.
     *
     * @param token the JWT token
     * @return the JTI
     */
    public String extractJti(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.getId();
        } catch (JwtException e) {
            log.error("Failed to extract JTI from token", e);
            return null;
        }
    }

    /**
     * Extract the subject (user ID) from a token.
     *
     * @param token the JWT token
     * @return the user ID
     */
    public UUID extractUserId(String token) {
        try {
            Claims claims = parseToken(token);
            return UUID.fromString(claims.getSubject());
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Failed to extract user ID from token", e);
            return null;
        }
    }

    /**
     * Check if a token is expired.
     *
     * @param token the JWT token
     * @return true if expired
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        } catch (JwtException e) {
            log.error("Failed to check token expiration", e);
            return true;
        }
    }

    /**
     * Validate a token.
     *
     * @param token the JWT token
     * @return true if valid
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return !isTokenExpired(token);
        } catch (JwtException e) {
            log.debug("Token validation failed: {}", e.getMessage());
            return false;
        }
    }
}
