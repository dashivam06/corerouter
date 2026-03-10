package com.fleebug.corerouter.service.token;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

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
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ServiceTokenService {

    private static final int TOKEN_ID_BYTES = 12;
    private static final int SECRET_BYTES = 32;
    private static final String TOKEN_PREFIX = "svc_";
    private static final String TOKEN_SEPARATOR = ".";

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

        String tokenId = generateRandomBase64Url(TOKEN_ID_BYTES);
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
        log.info("Service token created — name={}, role={}", name, role);

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
        log.debug("Service token authenticated — name={}, role={}", token.getName(), token.getRole());
        return token;
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
        log.info("Service token revoked — name={}", name);
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
        log.info("Service token activated — name={}", name);
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
        log.info("Service token deleted — name={}", name);
    }

    // ── internal ────────────────────────────────────────────────────────

    private String generateRandomBase64Url(int byteLength) {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[byteLength];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
