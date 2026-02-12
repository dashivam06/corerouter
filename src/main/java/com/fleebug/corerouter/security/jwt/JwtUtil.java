package com.fleebug.corerouter.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtUtil {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpirationMs;

    /**
     * Generate JWT token from user details
     * 
     * @param userId user ID
     * @param email user email
     * @param username username
     * @return generated JWT token
     */
    public String generateToken(Integer userId, String email, String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("email", email);
        claims.put("username", username);

        return createToken(claims, email);
    }

    /**
     * Create JWT token with claims
     * 
     * @param claims token claims
     * @param subject email subject
     * @return generated JWT token
     */
    private String createToken(Map<String, Object> claims, String subject) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * Extract email/subject from JWT token
     * 
     * @param token JWT token
     * @return email/subject from token
     */
    public String extractEmail(String token) {
        return extractClaims(token).getSubject();
    }

    /**
     * Extract user ID from JWT token
     * 
     * @param token JWT token
     * @return user ID from token
     */
    public Integer extractUserId(String token) {
        return extractClaims(token).get("userId", Integer.class);
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
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Validate JWT token
     * 
     * @param token JWT token
     * @return true if token is valid
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            log.error("JWT token validation failed: {}", e.getMessage());
            return false;
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
            log.error("Error checking token expiration: {}", e.getMessage());
            return true;
        }
    }
}
