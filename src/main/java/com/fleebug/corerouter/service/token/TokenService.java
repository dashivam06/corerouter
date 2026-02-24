package com.fleebug.corerouter.service.token;

import com.fleebug.corerouter.dto.user.response.AuthResponse;
import com.fleebug.corerouter.entity.token.UserToken;
import com.fleebug.corerouter.entity.user.User;
import com.fleebug.corerouter.enums.token.TokenType;
import com.fleebug.corerouter.repository.token.UserTokenRepository;
import com.fleebug.corerouter.security.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TokenService {

    private final UserTokenRepository userTokenRepository;
    private final JwtUtil jwtUtil;

    /**
     * Build auth response with access and refresh tokens
     * Saves both tokens to UserToken table
     * 
     * @param user User object
     * @return AuthResponse with tokens and expiration
     */
    public AuthResponse buildAuthResponse(User user) {
        log.info("Building auth response for user ID: {}", user.getUserId());
        
        // Generate access token
        String accessToken = jwtUtil.generateToken(user.getUserId(), user.getEmail(), user.getFullName(), user.getRole().toString());
        Long accessTokenExpiresIn = jwtUtil.getTokenExpirationTimeInMs(accessToken);
        
        // Save access token to UserToken table
        saveUserToken(user, accessToken, TokenType.ACCESS, new Date(System.currentTimeMillis() + accessTokenExpiresIn));
        
        // Generate refresh token (minimal claims: sub, iat, exp)
        String refreshToken = jwtUtil.generateRefreshToken(user.getUserId());
        Long refreshTokenExpiresIn = jwtUtil.getTokenExpirationTimeInMs(refreshToken);
        
        // Save refresh token to UserToken table
        saveUserToken(user, refreshToken, TokenType.REFRESH, new Date(System.currentTimeMillis() + refreshTokenExpiresIn));
        
        log.info("Auth response built successfully for user ID: {}", user.getUserId());
        
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(accessTokenExpiresIn)
                .build();
    }

    /**
     * Save token to UserToken table
     * 
     * @param user User object
     * @param token Token string
     * @param tokenType Access or Refresh
     * @param expiresAt Expiration date
     */
    private void saveUserToken(User user, String token, TokenType tokenType, Date expiresAt) {
        UserToken userToken = UserToken.builder()
                .user(user)
                .tokenType(tokenType)
                .tokenValue(token)
                .provider("LOCAL")
                .issuedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.ofInstant(expiresAt.toInstant(), ZoneId.systemDefault()))
                .revoked(false)
                .build();
        
        userTokenRepository.save(userToken);
        log.debug("Token saved to UserToken table. Type: {}, User ID: {}", tokenType, user.getUserId());
    }

    /**
     * Validate refresh token
     * 
     * @param refreshToken Refresh token string
     * @return true if valid
     */
    public boolean validateRefreshToken(String refreshToken) {
        try {
            UserToken userToken = userTokenRepository.findByTokenValue(refreshToken)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));
            
            if (!TokenType.REFRESH.equals(userToken.getTokenType())) {
                throw new IllegalArgumentException("Token is not a refresh token");
            }
            
            if (userToken.isRevoked() || LocalDateTime.now().isAfter(userToken.getExpiresAt())) {
                throw new IllegalArgumentException("Refresh token is expired or revoked");
            }
            
            return jwtUtil.validateToken(refreshToken);
        } catch (Exception e) {
            log.error("Refresh token validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get user from refresh token
     * 
     * @param refreshToken Refresh token string
     * @return User object
     */
    public User getUserFromRefreshToken(String refreshToken) {
        log.info("Extracting user from refresh token");
        
        UserToken userToken = userTokenRepository.findByTokenValue(refreshToken)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));
        
        if (!TokenType.REFRESH.equals(userToken.getTokenType())) {
            throw new IllegalArgumentException("Token is not a refresh token");
        }
        
        if (userToken.isRevoked() || LocalDateTime.now().isAfter(userToken.getExpiresAt())) {
            throw new IllegalArgumentException("Refresh token is expired or revoked");
        }
        
        log.info("User extracted from refresh token. User ID: {}", userToken.getUser().getUserId());
        return userToken.getUser();
    }

    /**
     * Generate new access token from user
     * 
     * @param user User object
     * @return new access token
     */
    public String generateAccessToken(User user) {
        log.info("Generating access token for user ID: {}", user.getUserId());
        return jwtUtil.generateToken(user.getUserId(), user.getEmail(), user.getFullName(), user.getRole().toString());
    }

    /**
     * Get access token expiration time in milliseconds
     * 
     * @param token access token
     * @return expiration time in milliseconds
     */
    public Long getAccessTokenExpirationTime(String token) {
        return jwtUtil.getTokenExpirationTimeInMs(token);
    }

    /**
     * Revoke refresh token
     * 
     * @param refreshToken Refresh token string
     */
    public void revokeRefreshToken(String refreshToken) {
        log.info("Revoking refresh token");
        
        UserToken userToken = userTokenRepository.findByTokenValue(refreshToken)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));
        
        userToken.setRevoked(true);
        userTokenRepository.save(userToken);
        
        log.info("Refresh token revoked successfully for user ID: {}", userToken.getUser().getUserId());
    }
}
