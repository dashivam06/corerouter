package com.fleebug.corerouter.service.token;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.SeverityLevel;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fleebug.corerouter.entity.token.ServiceToken;
import com.fleebug.corerouter.enums.token.ServiceRole;
import com.fleebug.corerouter.exception.token.InvalidServiceTokenException;
import com.fleebug.corerouter.exception.token.ServiceTokenAlreadyExistsException;
import com.fleebug.corerouter.exception.token.ServiceTokenNotFoundException;
import com.fleebug.corerouter.repository.token.ServiceTokenRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ServiceTokenService {

    private final TelemetryClient telemetryClient;

    private static final int TOKEN_ID_BYTES = 12;
    private static final int SECRET_BYTES = 32;
    private static final String TOKEN_PREFIX = "svc_";
    private static final String TOKEN_SEPARATOR = ".";
    private static final int MAX_TOKEN_ID_GENERATION_ATTEMPTS = 10;

    private final ServiceTokenRepository serviceTokenRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Create a new service token. The raw token is returned ONCE — it cannot be
     * recovered after this call because only the BCrypt hash is persisted.
     *
     * @param name unique human-readable name (e.g. "ocr-worker-1")
     * @param role the role granted to this token
     * @return the raw token string (must be stored securely by the caller)
     */
    @Transactional
    public String createToken(String name, ServiceRole role) {
        if (serviceTokenRepository.existsByName(name)) {
            throw new ServiceTokenAlreadyExistsException(name);
        }

        String tokenId = generateUniqueTokenId();
        String secret = generateRandomBase64Url(SECRET_BYTES);
        String hash = passwordEncoder.encode(secret);

        ServiceToken entity = ServiceToken.builder()
                .tokenId(tokenId)
                .name(name)
                .tokenHash(hash)
                .role(role)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        serviceTokenRepository.save(entity);
        
        Map<String, String> properties = new HashMap<>();
        properties.put("name", name);
        properties.put("role", role.name());
        telemetryClient.trackTrace("Service token created", SeverityLevel.Information, properties);

        // Return full token: svc_<tokenId>.<secret>
        return TOKEN_PREFIX + tokenId + TOKEN_SEPARATOR + secret;
    }

    /**
     * Authenticate an incoming request by raw token.
     * Token format: svc_<tokenId>.<secret>
     * Looks up by tokenId (O(1) DB hit), then verifies the secret against the hash.
     */
    @Transactional
    public ServiceToken authenticate(String rawToken) {
        if (rawToken == null || !rawToken.startsWith(TOKEN_PREFIX)) {
            throw new InvalidServiceTokenException("Malformed service token");
        }

        // Strip prefix: "abc123.XYZsecret"
        String withoutPrefix = rawToken.substring(TOKEN_PREFIX.length());
        int dotIndex = withoutPrefix.indexOf(TOKEN_SEPARATOR);
        if (dotIndex <= 0 || dotIndex == withoutPrefix.length() - 1) {
            throw new InvalidServiceTokenException("Malformed service token");
        }

        String tokenId = withoutPrefix.substring(0, dotIndex);
        String secret = withoutPrefix.substring(dotIndex + 1);

        ServiceToken token = serviceTokenRepository.findByTokenId(tokenId)
                .orElseThrow(InvalidServiceTokenException::new);

        if (!token.isActive()) {
            throw new InvalidServiceTokenException("Service token has been revoked");
        }

        if (!passwordEncoder.matches(secret, token.getTokenHash())) {
            throw new InvalidServiceTokenException();
        }

        token.setLastUsedAt(LocalDateTime.now());
        serviceTokenRepository.save(token);
        
        Map<String, String> properties = new HashMap<>();
        properties.put("name", token.getName());
        properties.put("role", token.getRole().name());
        // telemetryClient.trackTrace("Service token authenticated", SeverityLevel.Verbose, properties);
        
        return token;
    }

    /**
     * Get a service token by tokenId.
     */
    @Transactional(readOnly = true)
    public ServiceToken getByTokenId(String tokenId) {
        return serviceTokenRepository.findByTokenId(tokenId)
                .orElseThrow(() -> new ServiceTokenNotFoundException(tokenId));
    }

    /**
     * Get a service token by unique name.
     */
    @Transactional(readOnly = true)
    public ServiceToken getByName(String name) {
        return serviceTokenRepository.findByName(name)
                .orElseThrow(() -> new ServiceTokenNotFoundException(name));
    }

    /**
     * Revoke (deactivate) a service token by name.
     */
    @Transactional
    public void revokeToken(String name) {
        ServiceToken token = serviceTokenRepository.findByName(name)
                .orElseThrow(() -> new ServiceTokenNotFoundException(name));

        token.setActive(false);
        serviceTokenRepository.save(token);
        telemetryClient.trackTrace("Service token revoked", SeverityLevel.Information, Collections.singletonMap("name", name));
    }

    /**
     * Revoke (deactivate) a service token by tokenId.
     */
    @Transactional
    public void revokeTokenByTokenId(String tokenId) {
        ServiceToken token = getByTokenId(tokenId);
        token.setActive(false);
        serviceTokenRepository.save(token);
        telemetryClient.trackTrace("Service token revoked", SeverityLevel.Information, Collections.singletonMap("tokenId", tokenId));
    }

    /**
     * Re-activate a previously revoked token.
     */
    @Transactional
    public void activateToken(String name) {
        ServiceToken token = serviceTokenRepository.findByName(name)
                .orElseThrow(() -> new ServiceTokenNotFoundException(name));

        token.setActive(true);
        serviceTokenRepository.save(token);
        telemetryClient.trackTrace("Service token activated", SeverityLevel.Information, Collections.singletonMap("name", name));
    }

    /**
     * Re-activate a previously revoked token by tokenId.
     */
    @Transactional
    public void activateTokenByTokenId(String tokenId) {
        ServiceToken token = getByTokenId(tokenId);
        token.setActive(true);
        serviceTokenRepository.save(token);
        telemetryClient.trackTrace("Service token activated", SeverityLevel.Information, Collections.singletonMap("tokenId", tokenId));
    }

    /**
     * List all service tokens (metadata only — hashes are never exposed).
     */
    @Transactional(readOnly = true)
    public List<ServiceToken> listAll() {
        return serviceTokenRepository.findAll();
    }

    /**
     * List active tokens filtered by role.
     */
    @Transactional(readOnly = true)
    public List<ServiceToken> listActiveByRole(ServiceRole role) {
        return serviceTokenRepository.findByRoleAndActiveTrue(role);
    }

    /**
     * Delete a service token permanently.
     */
    @Transactional
    public void deleteToken(String name) {
        ServiceToken token = serviceTokenRepository.findByName(name)
                .orElseThrow(() -> new ServiceTokenNotFoundException(name));

        serviceTokenRepository.delete(token);
        telemetryClient.trackTrace("Service token deleted", SeverityLevel.Information, Collections.singletonMap("name", name));
    }

    /**
     * Delete a service token permanently by tokenId.
     */
    @Transactional
    public void deleteTokenByTokenId(String tokenId) {
        ServiceToken token = getByTokenId(tokenId);
        serviceTokenRepository.delete(token);
        telemetryClient.trackTrace("Service token deleted", SeverityLevel.Information, Collections.singletonMap("tokenId", tokenId));
    }

    // ── internal ────────────────────────────────────────────────────────

    private String generateRandomBase64Url(int byteLength) {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[byteLength];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String generateUniqueTokenId() {
        for (int i = 0; i < MAX_TOKEN_ID_GENERATION_ATTEMPTS; i++) {
            String candidate = generateRandomBase64Url(TOKEN_ID_BYTES);
            if (!serviceTokenRepository.existsByTokenId(candidate)) {
                return candidate;
            }
        }

        throw new IllegalStateException("Unable to generate a unique service token ID");
    }
}
