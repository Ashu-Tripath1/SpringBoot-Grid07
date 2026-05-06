package com.grid07.scheduler;

import com.grid07.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * CRON sweeper that periodically drains pending notification lists from Redis
 * and delivers summarised alerts to users.
 *
 * <p>The schedule is configured in {@code application.yml} under
 * {@code scheduling.notification-sweeper.cron} (default: every 5 minutes).
 * In production this would be 15 minutes; 5 minutes is used here to make
 * manual testing observable without a long wait.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationSweeper {

    private final NotificationService notificationService;

    /**
     * Triggered by the Spring scheduler every 5 minutes.
     *
     * <p>The {@code cron} expression {@code "0 *&#47;5 * * * *"} fires at second 0
     * of every 5th minute.  Spring's CRON format is:
     * {@code second minute hour day-of-month month day-of-week}.</p>
     */
    @Scheduled(cron = "${scheduling.notification-sweeper.cron}")
    public void runSweep() {
        log.info("=== Notification Sweeper: starting sweep cycle ===");
        long startMs = System.currentTimeMillis();

        try {
            notificationService.sweepAndSendPendingNotifications();
        } catch (Exception ex) {
            // Swallow exceptions so a Redis blip doesn't stop future sweeps
            log.error("Notification sweeper encountered an error during sweep cycle", ex);
        }

        long elapsedMs = System.currentTimeMillis() - startMs;
        log.info("=== Notification Sweeper: sweep cycle complete in {}ms ===", elapsedMs);
    }
}
