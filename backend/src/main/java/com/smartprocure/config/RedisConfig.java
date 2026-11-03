package com.smartprocure.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis {@link RedisTemplate} configuration.
 *
 * <p>Serialisation strategy:
 * <ul>
 *   <li>Keys   → {@link StringRedisSerializer}  (human-readable in Redis CLI)</li>
 *   <li>Values → {@link GenericJackson2JsonRedisSerializer} (JSON, type metadata included)</li>
 * </ul>
 *
 * <p>The bean is conditional on {@link RedisConnectionFactory} being present.
 * When the {@code test} profile excludes Redis auto-configuration, no
 * {@code RedisConnectionFactory} is available and this bean is skipped,
 * preventing test failures for offline integration tests.
 */
@Configuration
public class RedisConfig {

    @Bean
    @ConditionalOnBean(RedisConnectionFactory.class)
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();

        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(jsonSerializer);
        template.setDefaultSerializer(jsonSerializer);
        template.afterPropertiesSet();

        return template;
    }
}
