package com.fleebug.corerouter.config;

import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;


@Configuration
public class RedisConfig {

    private static final Logger logger = LoggerFactory.getLogger(RedisConfig.class);

    /**
     * RedisTemplate for generic operations related to custom classes 
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory, ObjectMapper objectMapper) {
        try {
            // Check if connection factory is available
            if (connectionFactory == null) {
                logger.error("Redis connection factory is not available. Please ensure Redis server is running and configured properly.");
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
            logger.info("RedisTemplate bean initialized successfully.");
            return template;
        } catch (IllegalStateException e) {
            logger.error("Failed to initialize RedisTemplate. Error: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error while initializing RedisTemplate. Error: {}", e.getMessage(), e);
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
                logger.error("Redis connection factory is not available for StringRedisTemplate. Please ensure Redis server is running and configured properly.");
                throw new IllegalStateException("Redis connection factory cannot be null. Redis connection is not established.");
            }

            StringRedisTemplate template = new StringRedisTemplate(connectionFactory);
            logger.info("StringRedisTemplate bean initialized successfully.");
            return template;
        } catch (IllegalStateException e) {
            logger.error("Failed to initialize StringRedisTemplate. Error: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error while initializing StringRedisTemplate. Error: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initialize StringRedisTemplate due to: " + e.getMessage(), e);
        }
    }
}
