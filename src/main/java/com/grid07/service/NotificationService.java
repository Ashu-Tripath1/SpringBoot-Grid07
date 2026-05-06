package com.grid07.service;

/**
 * Throttled notification delivery for bot-to-user interactions.
 *
 * <p>Instead of bombarding users with individual bot-activity alerts, this
 * service batches notifications within a 15-minute sliding window and
 * delivers a single summary via the CRON sweeper.</p>
 */
public interface NotificationService {

    /**
     * Attempts to deliver an immediate push notification to the user.
     *
     * <ul>
     *   <li>If the user is <em>not</em> on cooldown: logs the notification to
     *       the console and sets a 15-minute cooldown key in Redis.</li>
     *   <li>If the user <em>is</em> on cooldown: pushes the notification string
     *       onto the user's pending notifications Redis List for later batch
     *       delivery.</li>
     * </ul>
     *
     * @param userId           the user to notify
     * @param notificationText human-readable notification body (e.g. "Bot Alpha replied to your post")
     */
    void handleBotInteractionNotification(Long userId, String notificationText);

    /**
     * Sweeps all pending notification lists and delivers summarised messages.
     *
     * <p>Called by the {@code @Scheduled} CRON sweeper every 5 minutes.
     * For each user with pending notifications the method atomically pops
     * all messages, constructs a summary, and logs it to the console.</p>
     */
    void sweepAndSendPendingNotifications();
}
