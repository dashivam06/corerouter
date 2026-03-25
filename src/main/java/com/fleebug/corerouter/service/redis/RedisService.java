package com.fleebug.corerouter.service.redis;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.SeverityLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Generic Redis service for queue operations and caching
 * Can be used for:
 * - Publishing messages to queues (email, notifications, etc.)
 * - Caching data with TTL
 * - General key-value operations
 */
@Service
@RequiredArgsConstructor
public class RedisService {

    private final TelemetryClient telemetryClient;
    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * Publish a message to a Redis queue/channel
     * 
     * @param queueName The queue/channel name
     * @param message   The message to publish
     */
    public void publishToQueue(String queueName, String message) {
        try {
            stringRedisTemplate.opsForList().leftPush(queueName, message);
            
            Map<String, String> properties = new HashMap<>();
            properties.put("queue", queueName);
            telemetryClient.trackTrace("Job added to queue", SeverityLevel.Information, properties);
        } catch (Exception e) {
            Map<String, String> properties = new HashMap<>();
            properties.put("queue", queueName);
            telemetryClient.trackException(e, properties, null);
            throw new RuntimeException("Queue push failed", e);
        }
    }


    /**
     * Save data to Redis cache with TTL
     * 
     * @param key          The cache key
     * @param value        The value to cache
     * @param ttl          The time to live
     * @param timeUnit     The unit of ttl (MINUTES, SECONDS, etc.)
     */
    public void saveToCache(String key, String value, long ttl, TimeUnit timeUnit) {
        try {
            stringRedisTemplate.opsForValue().set(key, value, ttl, timeUnit);
            
            // Removing debug log for performance, only tracking errors or significant events
            // if needed, use Verbose level trace
            // Map<String, String> properties = new HashMap<>();
            // properties.put("key", key);
            // properties.put("ttl", String.valueOf(ttl));
            // telemetryClient.trackTrace("Cached key", SeverityLevel.Verbose, properties);
        } catch (Exception e) {
            Map<String, String> properties = new HashMap<>();
            properties.put("key", key);
            telemetryClient.trackException(e, properties, null);
            throw new RuntimeException("Failed to save to cache: " + key, e);
        }
    }

    /**
     * Save object to Redis cache with TTL
     * 
     * @param key          The cache key
     * @param value        The object to cache
     * @param ttl          The time to live
     * @param timeUnit     The unit of ttl
     */
    public void saveObjectToCache(String key, Object value, long ttl, TimeUnit timeUnit) {
        try {
            redisTemplate.opsForValue().set(key, value, ttl, timeUnit);
        } catch (Exception e) {
            Map<String, String> properties = new HashMap<>();
            properties.put("key", key);
            telemetryClient.trackException(e, properties, null);
            throw new RuntimeException("Failed to save object to cache: " + key, e);
        }
    }

    /**
     * Retrieve value from cache
     * 
     * @param key The cache key
     * @return The cached value or null if not found
     */
    public String getFromCache(String key) {
        try {
            return stringRedisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            Map<String, String> properties = new HashMap<>();
            properties.put("key", key);
            telemetryClient.trackException(e, properties, null);
            return null;
        }
    }

    /**
     * Retrieve object from cache
     * 
     * @param key The cache key
     * @return The cached object or null if not found
     */
    public Object getObjectFromCache(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            Map<String, String> properties = new HashMap<>();
            properties.put("key", key);
            telemetryClient.trackException(e, properties, null);
            return null;
        }
    }

    /**
     * Delete key from cache
     * 
     * @param key The cache key
     */
    public void deleteFromCache(String key) {
        try {
            stringRedisTemplate.delete(key);
        } catch (Exception e) {
            Map<String, String> properties = new HashMap<>();
            properties.put("key", key);
            telemetryClient.trackException(e, properties, null);
            throw new RuntimeException("Failed to delete from cache: " + key, e);
        }
    }

    /**
     * Check if key exists in cache
     * 
     * @param key The cache key
     * @return true if exists, false otherwise
     */
    public boolean existsInCache(String key) {
        try {
            return Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));
        } catch (Exception e) {
            Map<String, String> properties = new HashMap<>();
            properties.put("key", key);
            telemetryClient.trackException(e, properties, null);
            return false;
        }
    }

    /**
     * Get TTL of a key
     * 
     * @param key The cache key
     * @return TTL in seconds, or -1 if no expiration, -2 if key doesn't exist
     */
    public long getTTL(String key) {
        try {
            Long ttl = stringRedisTemplate.getExpire(key, TimeUnit.SECONDS);
            return ttl != null ? ttl : -2;
        } catch (Exception e) {
            Map<String, String> properties = new HashMap<>();
            properties.put("key", key);
            telemetryClient.trackException(e, properties, null);
            return -2;
        }
    }

    /**
     * Increment a counter in cache (useful for attempt tracking)
     * 
     * @param key The cache key
     * @return The new value
     */
    public long incrementCounter(String key) {
        try {
            return stringRedisTemplate.opsForValue().increment(key);
        } catch (Exception e) {
            Map<String, String> properties = new HashMap<>();
            properties.put("key", key);
            telemetryClient.trackException(e, properties, null);
            throw new RuntimeException("Failed to increment counter: " + key, e);
        }
    }

    /**
     * Set counter with TTL
     * 
     * @param key      The cache key
     * @param ttl      Time to live
     * @param timeUnit Unit of ttl
     */
    public void setCounterWithTTL(String key, long ttl, TimeUnit timeUnit) {
        try {
            stringRedisTemplate.opsForValue().set(key, "0", ttl, timeUnit);
        } catch (Exception e) {
            Map<String, String> properties = new HashMap<>();
            properties.put("key", key);
            telemetryClient.trackException(e, properties, null);
            throw new RuntimeException("Failed to set counter: " + key, e);
        }
    }

    /**
     * Append a record to a Redis Stream.
     *
     * @param streamKey The stream key (e.g. "stream:tasks")
     * @param fields    The entry fields
     */
    public void appendToStream(String streamKey, Map<String, String> fields) {
        try {
            MapRecord<String, String, String> record = StreamRecords
                    .mapBacked(fields)
                    .withStreamKey(streamKey);

            stringRedisTemplate.opsForStream().add(record);
            
            // Map<String, String> properties = new HashMap<>();
            // properties.put("streamKey", streamKey);
            // telemetryClient.trackTrace("Appended record to Redis Stream", SeverityLevel.Verbose, properties);
        } catch (Exception e) {
            Map<String, String> properties = new HashMap<>();
            properties.put("streamKey", streamKey);
            telemetryClient.trackException(e, properties, null);
            throw new RuntimeException("Failed to append record to Redis Stream", e);
        }
    }
}
