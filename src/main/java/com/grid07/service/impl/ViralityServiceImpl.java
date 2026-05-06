package com.grid07.service.impl;

import com.grid07.config.RedisKeys;
import com.grid07.service.ViralityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Redis-backed implementation of {@link ViralityService}.
 *
 * <p>Each interaction atomically increments a Redis counter using
 * {@code INCRBY}, which is a O(1), single-round-trip, thread-safe operation.
 * No Java-level locking is required because Redis serialises all commands
 * on a single thread internally.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ViralityServiceImpl implements ViralityService {

    private final StringRedisTemplate redisTemplate;

    @Override
    public void recordInteraction(Long postId, InteractionType interactionType) {
        String key = RedisKeys.viralityScore(postId);
        Long newScore = redisTemplate.opsForValue().increment(key, interactionType.getPoints());

        log.debug("Virality score updated — post={}, interaction={}, newScore={}",
                postId, interactionType, newScore);
    }

    @Override
    public long getScore(Long postId) {
        String raw = redisTemplate.opsForValue().get(RedisKeys.viralityScore(postId));
        if (raw == null) {
            return 0L;
        }
        return Long.parseLong(raw);
    }
}
