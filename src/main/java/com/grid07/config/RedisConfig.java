package com.grid07.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis infrastructure configuration.
 *
 * <p>We use {@link StringRedisTemplate} exclusively so that all keys and values
 * are plain UTF-8 strings — this keeps things inspectable with redis-cli and
 * avoids Java-specific serialisation bytes polluting the keyspace.</p>
 */
@Configuration
public class RedisConfig {

    /**
     * Provides a {@link StringRedisTemplate} wired with explicit string
     * serialisers on both key and value channels.  Spring Boot auto-configures
     * the {@link RedisConnectionFactory} from {@code application.yml}.
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    /**
     * Generic {@link RedisTemplate} kept for Lua script execution convenience.
     * Keys and values are still serialised as plain strings.
     */
    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }
}
