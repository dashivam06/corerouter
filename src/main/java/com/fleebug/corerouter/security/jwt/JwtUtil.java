package com.fleebug.corerouter.security.jwt;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.SeverityLevel;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fleebug.corerouter.enums.token.TokenValidationStatus;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final TelemetryClient telemetryClient;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpirationMs;

    @Value("${jwt.refresh-expiration:604800000}")
    private long refreshTokenExpirationMs;

    /**
     * Generate JWT token from user details
     * 
     * @param userId user ID
     * @param email user email
     * @param username username
     * @return generated JWT token
     */
    public String generateToken(Integer userId, String email, String username, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("email", email);
        claims.put("username", username);
        claims.put("role", role);

        return createToken(claims, userId.toString());
    }

    /**
     * Create JWT token with claims
     * 
     * @param claims token claims
     * @param subject userId subject
     * @return generated JWT token
     */
    private String createToken(Map<String, Object> claims, String subject) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey() ,Jwts.SIG.HS512)
                .compact();
        
    }

    public String extractUserId(String token) {
    return extractClaims(token).getSubject(); 
    }

    public String extractEmailFromClaims(String token) {
        return extractClaims(token).get("email", String.class); 
    }

    /**
     * Extract username from JWT token
     * 
     * @param token JWT token
     * @return username from token
     */
    public String extractUsername(String token) {
        return extractClaims(token).get("username", String.class);
    }

    /**
     * Extract all claims from JWT token
     * 
     * @param token JWT token
     * @return claims from token
     */
    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Validate JWT token
     * 
     * @param token JWT token
     * @return true if token is valid
     */
    public boolean validateToken(String token) {
        return getTokenValidationStatus(token) == TokenValidationStatus.VALID;
    }

    public TokenValidationStatus getTokenValidationStatus(String token) {
        try {
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7).trim();
            }

            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);

            return TokenValidationStatus.VALID;
        } catch (ExpiredJwtException e) {
            Map<String, String> properties = new HashMap<>();
            properties.put("error", e.getMessage());
            telemetryClient.trackTrace("JWT token expired", SeverityLevel.Warning, properties);
            return TokenValidationStatus.EXPIRED;
        } catch (JwtException | IllegalArgumentException e) {
            Map<String, String> properties = new HashMap<>();
            properties.put("error", e.getMessage());
            telemetryClient.trackTrace("JWT token validation failed", SeverityLevel.Warning, properties);
            return TokenValidationStatus.INVALID;
        }
    }

    /**
     * Get signing key from secret
     * 
     * @return signing key
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Check if token is expired
     * 
     * @param token JWT token
     * @return true if token is expired
     */
    public boolean isTokenExpired(String token) {
        try {
            return extractClaims(token).getExpiration().before(new Date());
        } catch (Exception e) {
            Map<String, String> properties = new HashMap<>();
            properties.put("error", e.getMessage());
            telemetryClient.trackException(e, properties, null);
            return true;
        }
    }

    /**
     * Generate refresh token with minimal claims
     * 
     * @param userId user ID
     * @return generated refresh token
     */
    public String generateRefreshToken(Integer userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenExpirationMs);

        return Jwts.builder()
                .subject(userId.toString())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey() ,Jwts.SIG.HS512)
                .compact();
    }

    /**
     * Get token expiration time in milliseconds
     * 
     * @param token JWT token
     * @return expiration time in milliseconds from now
     */
    public Long getTokenExpirationTimeInMs(String token) {
        try {
            return extractClaims(token).getExpiration().getTime() - System.currentTimeMillis();
        } catch (Exception e) {
            Map<String, String> properties = new HashMap<>();
            properties.put("error", e.getMessage());
            telemetryClient.trackException(e, properties, null);
            return 0L;
        }
    }
}
