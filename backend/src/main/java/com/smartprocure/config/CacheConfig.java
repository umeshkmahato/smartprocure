package com.smartprocure.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Map;

/**
 * Redis-backed cache configuration.
 *
 * <p>Cache strategy:
 * <ul>
 *   <li>{@code users}              – 10 min TTL (authentication lookups)</li>
 *   <li>{@code vendors}            – 5 min TTL (vendor list/detail pages)</li>
 *   <li>{@code approvalThresholds} – 30 min TTL (rarely changed admin config)</li>
 *   <li>{@code dashboard}          – 15 min TTL (report aggregations)</li>
 *   <li>Default                    – 5 min TTL</li>
 * </ul>
 *
 * <p>The {@link CacheManager} bean is conditional on {@link RedisConnectionFactory}
 * being present. In the {@code test} profile, Redis auto-configuration is excluded
 * via {@code application-test.yml}, so Spring's no-op cache is used instead,
 * keeping unit and integration tests fast and infra-free.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String USERS_CACHE              = "users";
    public static final String VENDORS_CACHE            = "vendors";
    public static final String APPROVAL_THRESHOLDS_CACHE = "approvalThresholds";
    public static final String DASHBOARD_CACHE          = "dashboard";
    public static final String PURCHASE_REQUESTS_CACHE  = "purchaseRequests";
    public static final String VENDOR_RATINGS_CACHE     = "vendorRatings";

    @Bean
    @Primary
    @ConditionalOnBean(RedisConnectionFactory.class)
    public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration
                .defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()));

        Map<String, RedisCacheConfiguration> perCacheConfig = Map.of(
                USERS_CACHE,               defaultConfig.entryTtl(Duration.ofMinutes(10)),
                VENDORS_CACHE,             defaultConfig.entryTtl(Duration.ofMinutes(5)),
                APPROVAL_THRESHOLDS_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(30)),
                DASHBOARD_CACHE,           defaultConfig.entryTtl(Duration.ofMinutes(15)),
                PURCHASE_REQUESTS_CACHE,   defaultConfig.entryTtl(Duration.ofMinutes(5)),
                VENDOR_RATINGS_CACHE,      defaultConfig.entryTtl(Duration.ofMinutes(10))
        );

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(perCacheConfig)
                .build();
    }
}
