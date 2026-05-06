package com.grid07.config;

/**
 * Central registry of every Redis key pattern used by the application.
 *
 * <p>Having all key templates in one place prevents typos, makes it trivial
 * to spot collisions, and gives ops engineers a single reference when
 * inspecting the keyspace in production.</p>
 */
public final class RedisKeys {

    private RedisKeys() { /* utility class — do not instantiate */ }

    // ─── Virality ───────────────────────────────────────────────────────────

    /** Running virality score for a post. Value is an integer string. */
    public static String viralityScore(Long postId) {
        return String.format("post:%d:virality_score", postId);
    }

    // ─── Guardrail counters ─────────────────────────────────────────────────

    /**
     * Total number of bot replies on a post.
     * Incremented atomically; capped at {@code 100}.
     */
    public static String botReplyCount(Long postId) {
        return String.format("post:%d:bot_count", postId);
    }

    // ─── Cooldown keys ──────────────────────────────────────────────────────

    /**
     * Cooldown sentinel for a (bot, human) interaction pair.
     * Key presence means the cooldown is active; TTL = 10 minutes.
     */
    public static String botHumanCooldown(Long botId, Long humanId) {
        return String.format("cooldown:bot_%d:human_%d", botId, humanId);
    }

    // ─── Notification throttle ──────────────────────────────────────────────

    /**
     * Sentinel that indicates a real-time push notification was recently sent
     * to this user.  TTL = 15 minutes.
     */
    public static String userNotificationCooldown(Long userId) {
        return String.format("notif_cooldown:user_%d", userId);
    }

    /**
     * Redis List of pending (batched) notification strings for a user that is
     * currently inside its 15-minute notification cooldown window.
     */
    public static String userPendingNotifications(Long userId) {
        return String.format("user:%d:pending_notifs", userId);
    }

    /**
     * Glob pattern used by the CRON sweeper to discover all users that have
     * pending notifications queued.
     */
    public static final String PENDING_NOTIF_SCAN_PATTERN = "user:*:pending_notifs";
}
