package com.fleebug.corerouter.config;

import tools.jackson.databind.ObjectMapper;
import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.SeverityLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class RedisConfig {

    private final TelemetryClient telemetryClient;

    /**
     * RedisTemplate for generic operations related to custom classes 
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory, ObjectMapper objectMapper) {
        try {
            // Check if connection factory is available
            if (connectionFactory == null) {
                telemetryClient.trackTrace("Redis connection factory is not available", SeverityLevel.Error, null);
                throw new IllegalStateException("Redis connection factory cannot be null. Redis connection is not established.");
            }

            RedisTemplate<String, Object> template = new RedisTemplate<>();
            template.setConnectionFactory(connectionFactory);

            // String serializer for keys
            StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();

            // JSON serializer for values
            GenericJacksonJsonRedisSerializer jsonRedisSerializer = new GenericJacksonJsonRedisSerializer(objectMapper);

            // Set string serializer
            template.setKeySerializer(stringRedisSerializer);
            template.setValueSerializer(jsonRedisSerializer);

            // Set hash key/value serializer
            template.setHashKeySerializer(stringRedisSerializer);
            template.setHashValueSerializer(jsonRedisSerializer);

            template.afterPropertiesSet();
            // telemetryClient.trackTrace("RedisTemplate bean initialized successfully", SeverityLevel.Verbose, null);
            return template;
        } catch (IllegalStateException e) {
            Map<String, String> properties = new HashMap<>();
            properties.put("error", e.getMessage());
            telemetryClient.trackException(e, properties, null);
            throw e;
        } catch (Exception e) {
            Map<String, String> properties = new HashMap<>();
            properties.put("error", e.getMessage());
            telemetryClient.trackException(e, properties, null);
            throw new RuntimeException("Failed to initialize RedisTemplate due to: " + e.getMessage(), e);
        }
    }

    /**
     * StringRedisTemplate for string-based operations
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        try {
            // Check if connection factory is available
            if (connectionFactory == null) {
                telemetryClient.trackTrace("Redis connection factory is not available for StringRedisTemplate", SeverityLevel.Error, null);
                throw new IllegalStateException("Redis connection factory cannot be null. Redis connection is not established.");
            }

            StringRedisTemplate template = new StringRedisTemplate(connectionFactory);
            // telemetryClient.trackTrace("StringRedisTemplate bean initialized successfully", SeverityLevel.Verbose, null);
            return template;
        } catch (IllegalStateException e) {
            Map<String, String> properties = new HashMap<>();
            properties.put("error", e.getMessage());
            telemetryClient.trackException(e, properties, null);
            throw e;
        } catch (Exception e) {
            Map<String, String> properties = new HashMap<>();
            properties.put("error", e.getMessage());
            telemetryClient.trackException(e, properties, null);
            throw new RuntimeException("Failed to initialize StringRedisTemplate due to: " + e.getMessage(), e);
        }
    }
}
