package com.fleebug.corerouter.service.redis;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import jakarta.annotation.PostConstruct;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.stereotype.Service;

import com.fleebug.corerouter.config.RateLimitConfig;

import java.util.function.Supplier;

/**
 * Service to manage Bucket4j buckets backed by Redis.
 * Each bucket is identified by a unique key (IP or email).
 * The state of the bucket (tokens, refill time) is stored in Redis for distributed consistency.
 */
@Service
public class RedisBucketService {

    private final RateLimitConfig rateLimitConfig;
    private final LettuceConnectionFactory lettuceConnectionFactory;
    private ProxyManager<String> proxyManager;

    public RedisBucketService(RateLimitConfig rateLimitConfig,
                              LettuceConnectionFactory lettuceConnectionFactory) {
        this.rateLimitConfig = rateLimitConfig;
        this.lettuceConnectionFactory = lettuceConnectionFactory;
    }

    @PostConstruct
    public void init() {
        RedisClient redisClient = (RedisClient) lettuceConnectionFactory.getNativeClient();
        StatefulRedisConnection<String, byte[]> connection =
                redisClient.connect(RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE));
        this.proxyManager = LettuceBasedProxyManager.builderFor(connection).build();
    }

    public Bucket resolveOtpIpBucket(String clientIp) {
        Supplier<BucketConfiguration> config = () -> BucketConfiguration.builder()
                .addLimit(rateLimitConfig.otpIpBandwidth())
                .build();
        return proxyManager.builder().build("rl:otp:ip:" + clientIp, config);
    }

    public Bucket resolveOtpEmailBucket(String email) {
        Supplier<BucketConfiguration> config = () -> BucketConfiguration.builder()
                .addLimit(rateLimitConfig.otpEmailBandwidth())
                .build();
        return proxyManager.builder().build("rl:otp:email:" + email, config);
    }

    public Bucket resolveLoginIpBucket(String clientIp) {
        Supplier<BucketConfiguration> config = () -> BucketConfiguration.builder()
                .addLimit(rateLimitConfig.loginIpBandwidth())
                .build();
        // Uses simple-bucket keys
        return proxyManager.builder().build("rl:login:ip:" + clientIp, config);
    }

    public Bucket resolveVerifyIpBucket(String clientIp) {
        Supplier<BucketConfiguration> config = () -> BucketConfiguration.builder()
                .addLimit(rateLimitConfig.verifyIpBandwidth())
                .build();
        return proxyManager.builder().build("rl:auth:verify:ip:" + clientIp, config);
    }

    public Bucket resolveRefreshIpBucket(String clientIp) {
        Supplier<BucketConfiguration> config = () -> BucketConfiguration.builder()
                .addLimit(rateLimitConfig.refreshIpBandwidth())
                .build();
        return proxyManager.builder().build("rl:auth:refresh:ip:" + clientIp, config);
    }

    public Bucket resolveTaskCreationUserBucket(String userKey) {
        Supplier<BucketConfiguration> config = () -> BucketConfiguration.builder()
                .addLimit(rateLimitConfig.taskCreationUserBandwidth())
                .build();
        return proxyManager.builder().build("rl:task:create:user:" + userKey, config);
    }
    
    public Bucket resolveChatApiKeyBucket(String apiKeyHash) {
        Supplier<BucketConfiguration> config = () -> BucketConfiguration.builder()
                .addLimit(rateLimitConfig.chatApiKeyBandwidth())
                .build();
        return proxyManager.builder().build("rl:chat:apikey:" + apiKeyHash, config);
    }
}