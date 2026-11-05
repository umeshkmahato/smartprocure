package com.smartprocure.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Redis-backed idempotency guard for Kafka event consumers.
 *
 * <p>Uses SET NX (set-if-not-exists) with a configurable TTL so that
 * duplicate deliveries of the same {@code eventId} are silently skipped
 * even across restarts, rebalances, or at-least-once redeliveries.
 *
 * <p>Default TTL is 7 days — well beyond any realistic retry window.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private static final String KEY_PREFIX = "notif:processed:";

    private final StringRedisTemplate redisTemplate;

    @Value("${app.notification.idempotency-ttl-days:7}")
    private long ttlDays;

    /**
     * Returns {@code true} and marks the eventId as processed if this is
     * the first time it has been seen.  Returns {@code false} if it was
     * already processed (duplicate — caller should skip processing).
     */
    public boolean tryMarkProcessed(String eventId) {
        String key = KEY_PREFIX + eventId;
        Boolean inserted = redisTemplate.opsForValue()
                .setIfAbsent(key, "1", Duration.ofDays(ttlDays));
        boolean isNew = Boolean.TRUE.equals(inserted);
        if (!isNew) {
            log.warn("[Idempotency] Duplicate event skipped: eventId={}", eventId);
        }
        return isNew;
    }
}
