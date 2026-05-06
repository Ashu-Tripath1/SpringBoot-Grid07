package com.grid07.service.impl;

import com.grid07.config.RedisKeys;
import com.grid07.exception.GuardrailException;
import com.grid07.service.GuardrailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

/**
 * Redis-backed implementation of the three bot-interaction guardrails.
 *
 * <h2>Thread Safety — Lua Script Atomicity</h2>
 *
 * <p>The horizontal cap is the most concurrency-sensitive guardrail because
 * multiple threads can race to be the "100th" comment.  A naïve
 * {@code GET → check → INCR} sequence has a time-of-check/time-of-use (TOCTOU)
 * race: two threads can both read 99, both pass the check, and both increment
 * to 100 and 101 respectively — violating the cap.</p>
 *
 * <p>We solve this with an inline Lua script executed via
 * {@code EVAL}.  Redis guarantees that a Lua script runs atomically —
 * no other command can interleave between the {@code INCR} and the
 * comparison.  The script increments first, then checks: if the new value
 * exceeds the cap it immediately decrements to roll back and returns
 * {@code 0} (rejected); otherwise it returns {@code 1} (allowed).
 * This pattern is sometimes called an "optimistic atomic check-and-reserve."</p>
 *
 * <pre>
 * Redis EVAL guarantees:
 *   Thread A: INCR → 100 → 100 ≤ 100 → ALLOWED  (counter stays at 100)
 *   Thread B: INCR → 101 → 101 > 100 → DECR → REJECTED
 * </pre>
 *
 * <p>The cooldown cap uses {@code SET key value NX PX ttlMs}, which is itself
 * atomic: it sets the key <em>only if it does not already exist</em> and
 * returns {@code 1} on success, {@code null} on failure (key existed).
 * No Lua is needed here because the NX flag is natively atomic in Redis.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GuardrailServiceImpl implements GuardrailService {

    private static final int MAX_BOT_REPLIES  = 100;
    private static final int MAX_DEPTH_LEVEL  = 20;

    /**
     * Cooldown between a specific bot and a specific human: 10 minutes.
     */
    private static final Duration BOT_HUMAN_COOLDOWN = Duration.ofMinutes(10);

    private final StringRedisTemplate redisTemplate;

    /**
     * Lua script: atomically increment the bot-reply counter for a post.
     *
     * <p>Returns {@code 1L} if the slot was successfully reserved (counter ≤ cap),
     * or {@code 0L} if the cap was already reached (counter rolled back).</p>
     *
     * <pre>
     * KEYS[1] = post:{id}:bot_count
     * ARGV[1] = maximum allowed bot replies (100)
     * </pre>
     */
    private static final DefaultRedisScript<Long> ATOMIC_INCR_WITH_CAP_SCRIPT;

    static {
        ATOMIC_INCR_WITH_CAP_SCRIPT = new DefaultRedisScript<>();
        ATOMIC_INCR_WITH_CAP_SCRIPT.setResultType(Long.class);
        ATOMIC_INCR_WITH_CAP_SCRIPT.setScriptText(
            "local current = redis.call('INCR', KEYS[1]) " +
            "if current > tonumber(ARGV[1]) then " +
            "    redis.call('DECR', KEYS[1]) " +
            "    return 0 " +
            "end " +
            "return 1"
        );
    }

    // ─── Public API ──────────────────────────────────────────────────────────

    @Override
    public void enforceHorizontalCap(Long postId) {
        String counterKey = RedisKeys.botReplyCount(postId);
        List<String> keys = Collections.singletonList(counterKey);

        Long result = redisTemplate.execute(
                ATOMIC_INCR_WITH_CAP_SCRIPT,
                keys,
                String.valueOf(MAX_BOT_REPLIES)
        );

        if (result == null || result == 0L) {
            log.warn("Horizontal cap enforced — post={} has reached {} bot replies", postId, MAX_BOT_REPLIES);
            throw new GuardrailException(
                    "HORIZONTAL_CAP_EXCEEDED",
                    String.format("Post %d has already received the maximum of %d bot replies.",
                            postId, MAX_BOT_REPLIES)
            );
        }

        log.debug("Horizontal cap check passed — post={}, slot reserved via atomic Lua script", postId);
    }

    @Override
    public void enforceVerticalCap(int depthLevel) {
        if (depthLevel > MAX_DEPTH_LEVEL) {
            log.warn("Vertical cap enforced — requested depth {} exceeds maximum {}", depthLevel, MAX_DEPTH_LEVEL);
            throw new GuardrailException(
                    "VERTICAL_CAP_EXCEEDED",
                    String.format("Comment depth %d exceeds the maximum allowed depth of %d.",
                            depthLevel, MAX_DEPTH_LEVEL)
            );
        }
    }

    @Override
    public void enforceCooldownCap(Long botId, Long humanId) {
        String cooldownKey = RedisKeys.botHumanCooldown(botId, humanId);
        long ttlMillis = BOT_HUMAN_COOLDOWN.toMillis();

        /*
         * SET key "1" NX PX ttlMs
         *   → returns true  (Boolean) when the key was set (cooldown just started)
         *   → returns false (Boolean) when the key already existed (still on cooldown)
         *
         * This is a single atomic Redis command — no race condition is possible.
         */
        Boolean wasSet = redisTemplate.opsForValue()
                .setIfAbsent(cooldownKey, "1", BOT_HUMAN_COOLDOWN);

        if (Boolean.FALSE.equals(wasSet)) {
            long remainingSeconds = getRemainingTtlSeconds(cooldownKey);
            log.warn("Cooldown cap enforced — bot={} cannot interact with human={} for {}s more",
                    botId, humanId, remainingSeconds);
            throw new GuardrailException(
                    "COOLDOWN_CAP_ACTIVE",
                    String.format("Bot %d must wait %d more seconds before interacting with user %d again.",
                            botId, remainingSeconds, humanId)
            );
        }

        log.debug("Cooldown cap check passed — bot={}, human={}, cooldown set for {} minutes",
                botId, humanId, BOT_HUMAN_COOLDOWN.toMinutes());
    }

    @Override
    public void enforceAllBotCommentGuardrails(Long postId, Long botId, Long humanId, int depthLevel) {
        // Order matters: cheapest checks first to avoid unnecessary Redis round-trips.
        enforceVerticalCap(depthLevel);
        enforceCooldownCap(botId, humanId);
        enforceHorizontalCap(postId);  // Most expensive (Lua) — run last
    }

    // ─── Private helpers ─────────────────────────────────────────────────────

    private long getRemainingTtlSeconds(String key) {
        Long ttlMs = redisTemplate.getExpire(key);
        return ttlMs != null && ttlMs > 0 ? ttlMs : 0L;
    }
}
