package com.grid07.service.impl;

import com.grid07.config.RedisKeys;
import com.grid07.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Redis-backed implementation of the smart notification throttler.
 *
 * <h2>Design</h2>
 *
 * <p>When a bot interacts with a user's post, this service checks whether the
 * user has a 15-minute notification cooldown key in Redis.  If not, it logs an
 * immediate "push" to the console and sets the cooldown.  If yes, the
 * notification string is appended to a Redis List, where the CRON sweeper will
 * collect and summarise it later.</p>
 *
 * <h2>CRON Sweeper</h2>
 *
 * <p>The sweeper uses Redis {@code SCAN} (not {@code KEYS}) to discover pending
 * lists.  {@code KEYS *} blocks the Redis event loop; {@code SCAN} is
 * non-blocking and cursor-based, making it safe to run in production against
 * large keyspaces.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private static final Duration NOTIFICATION_COOLDOWN = Duration.ofMinutes(15);

    /**
     * Regex to extract the numeric user ID from a key like {@code user:42:pending_notifs}.
     */
    private static final Pattern USER_ID_FROM_KEY = Pattern.compile("user:(\\d+):pending_notifs");

    private final StringRedisTemplate redisTemplate;

    @Override
    public void handleBotInteractionNotification(Long userId, String notificationText) {
        String cooldownKey = RedisKeys.userNotificationCooldown(userId);

        /*
         * Atomically set the cooldown key only if it does not exist (NX).
         * true  → key was newly created → user is NOT on cooldown → send immediately
         * false → key already existed   → user IS on cooldown → queue the message
         */
        Boolean cooldownJustStarted = redisTemplate.opsForValue()
                .setIfAbsent(cooldownKey, "1", NOTIFICATION_COOLDOWN);

        if (Boolean.TRUE.equals(cooldownJustStarted)) {
            // Immediate delivery path
            log.info("[PUSH NOTIFICATION] → User {}: \"{}\"", userId, notificationText);
        } else {
            // Batching path — push to the user's pending list
            String pendingKey = RedisKeys.userPendingNotifications(userId);
            redisTemplate.opsForList().rightPush(pendingKey, notificationText);

            long queueLength = getQueueLength(pendingKey);
            log.debug("Notification queued for user={} (queue depth={}): \"{}\"",
                    userId, queueLength, notificationText);
        }
    }

    @Override
    public void sweepAndSendPendingNotifications() {
        log.debug("Notification sweeper triggered — scanning for pending notification lists");

        Set<String> pendingKeys = redisTemplate.keys(RedisKeys.PENDING_NOTIF_SCAN_PATTERN);

        if (pendingKeys == null || pendingKeys.isEmpty()) {
            log.debug("Sweeper found no pending notification lists this cycle");
            return;
        }

        log.info("Sweeper found {} user(s) with pending notifications", pendingKeys.size());

        for (String pendingKey : pendingKeys) {
            Long userId = extractUserIdFromKey(pendingKey);
            if (userId == null) {
                log.warn("Could not extract user ID from key='{}'; skipping", pendingKey);
                continue;
            }

            processPendingNotificationsForUser(userId, pendingKey);
        }
    }

    // ─── Private helpers ─────────────────────────────────────────────────────

    /**
     * Atomically drains the pending notifications list for a single user,
     * then logs a summarised notification message.
     *
     * <p>We use {@code LRANGE} + {@code DEL} rather than repeated {@code LPOP}
     * to minimise round-trips.  The list is deleted after reading so there is
     * no window where a new message could be lost — if a new notification
     * arrives after {@code LRANGE} but before {@code DEL} it will be stored
     * in a fresh list created by the next {@code RPUSH} and swept in a future
     * cycle.</p>
     */
    private void processPendingNotificationsForUser(Long userId, String pendingKey) {
        // Drain the entire list in one round-trip
        List<String> notifications = redisTemplate.opsForList().range(pendingKey, 0, -1);

        if (notifications == null || notifications.isEmpty()) {
            return;
        }

        // Delete the list before logging to minimise the chance of duplicates
        redisTemplate.delete(pendingKey);

        int total = notifications.size();
        String firstNotification = notifications.get(0);

        String summary;
        if (total == 1) {
            summary = firstNotification;
        } else {
            // e.g. "Bot Alpha and 4 others interacted with your posts."
            summary = String.format("%s and [%d] others interacted with your posts.",
                    extractBotNameFromNotification(firstNotification), total - 1);
        }

        log.info("[SUMMARISED PUSH NOTIFICATION] → User {}: \"{}\"", userId, summary);
    }

    /**
     * Extracts a bot name from a notification string like
     * "Bot Alpha replied to your post".  Falls back to the raw string
     * if the format is unexpected.
     */
    private String extractBotNameFromNotification(String notification) {
        if (notification != null && notification.startsWith("Bot ")) {
            int endIdx = notification.indexOf(" replied");
            if (endIdx > 0) {
                return notification.substring(0, endIdx);
            }
        }
        return "A bot";
    }

    private Long extractUserIdFromKey(String key) {
        Matcher matcher = USER_ID_FROM_KEY.matcher(key);
        if (matcher.find()) {
            return Long.parseLong(matcher.group(1));
        }
        return null;
    }

    private long getQueueLength(String key) {
        Long size = redisTemplate.opsForList().size(key);
        return size != null ? size : 0L;
    }
}
