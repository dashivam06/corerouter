package com.fleebug.corerouter.config;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Slf4j
@Service
@RequiredArgsConstructor
public class AzureTokenProvider {

    private static final String AZURE_INSIGHTS_SCOPE = "https://api.applicationinsights.io/.default";
    private static final long TOKEN_EXPIRY_BUFFER_SECONDS = 300; // Refresh 5 minutes before expiry

    @Value("${azure.entra.tenant-id}")
    private String tenantId;

    @Value("${azure.entra.client-id}")
    private String clientId;

    @Value("${azure.entra.client-secret}")
    private String clientSecret;

    private ClientSecretCredential credential;
    private String cachedToken;
    private long tokenExpiryTime = 0;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * Get a valid bearer token for Azure Application Insights API.
     * Tokens are cached and automatically refreshed when approaching expiry.
     */
    public String getToken() {
        lock.readLock().lock();
        try {
            // Check if cached token is still valid
            if (cachedToken != null && Instant.now().getEpochSecond() < tokenExpiryTime) {
                return cachedToken;
            }
        } finally {
            lock.readLock().unlock();
        }

        // Token expired or doesn't exist, acquire a new one
        lock.writeLock().lock();
        try {
            // Double-check inside write lock
            if (cachedToken != null && Instant.now().getEpochSecond() < tokenExpiryTime) {
                return cachedToken;
            }

            initializeCredentialIfNecessary();
            
            var tokenRequestContext = new com.azure.core.credential.TokenRequestContext()
                    .addScopes(AZURE_INSIGHTS_SCOPE);
            
            var accessToken = credential.getToken(tokenRequestContext).block();
            
            if (accessToken != null) {
                cachedToken = accessToken.getToken();
                // Set expiry buffer: refresh 5 minutes before actual expiry
                tokenExpiryTime = accessToken.getExpiresAt().toEpochSecond() - TOKEN_EXPIRY_BUFFER_SECONDS;
                log.debug("Successfully acquired new Azure token");
                return cachedToken;
            } else {
                log.error("Failed to acquire Azure token: token is null");
                throw new RuntimeException("Failed to acquire Azure token");
            }
        } catch (Exception e) {
            log.error("Error acquiring Azure token", e);
            throw new RuntimeException("Failed to acquire Azure Entra ID token", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void initializeCredentialIfNecessary() {
        if (credential == null) {
            credential = new ClientSecretCredentialBuilder()
                    .tenantId(tenantId)
                    .clientId(clientId)
                    .clientSecret(clientSecret)
                    .build();
            log.info("Azure ClientSecretCredential initialized");
        }
    }

    /**
     * Clear cached token to force refresh on next request.
     * Useful for manual token invalidation during development.
     */
    public void clearCache() {
        lock.writeLock().lock();
        try {
            cachedToken = null;
            tokenExpiryTime = 0;
            log.debug("Token cache cleared");
        } finally {
            lock.writeLock().unlock();
        }
    }
}
