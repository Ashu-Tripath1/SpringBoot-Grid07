package com.grid07.service;

import com.grid07.exception.GuardrailException;

/**
 * Enforces all Redis-backed rate-limiting guardrails for bot interactions.
 *
 * <p>All checks use atomic Redis operations to guarantee correctness under
 * concurrent load.  A failed check throws a {@link GuardrailException}
 * which propagates to the caller as HTTP 429 Too Many Requests.</p>
 */
public interface GuardrailService {

    /**
     * Attempts to reserve a bot-reply slot on the given post.
     *
     * <p>This method increments the {@code post:{id}:bot_count} key atomically
     * using a Lua script, then checks whether the new value exceeds
     * {@code MAX_BOT_REPLIES_PER_POST} (100).  If the cap is breached the
     * increment is <em>rolled back</em> and a {@link GuardrailException} is
     * thrown.</p>
     *
     * @param postId the post being replied to
     * @throws GuardrailException if the horizontal cap (100 bot replies) would be exceeded
     */
    void enforceHorizontalCap(Long postId);

    /**
     * Validates that the requested comment depth does not exceed the maximum
     * of 20 levels.
     *
     * @param depthLevel the requested nesting depth
     * @throws GuardrailException if {@code depthLevel > 20}
     */
    void enforceVerticalCap(int depthLevel);

    /**
     * Checks the per-bot, per-human interaction cooldown.
     *
     * <p>If no cooldown key exists for the {@code (botId, humanId)} pair the
     * method sets one with a 10-minute TTL and returns normally.  If the key
     * already exists a {@link GuardrailException} is thrown.</p>
     *
     * @param botId   the bot attempting to interact
     * @param humanId the human whose content is being interacted with
     * @throws GuardrailException if the cooldown is still active
     */
    void enforceCooldownCap(Long botId, Long humanId);

    /**
     * Convenience method that runs all three guardrails in sequence.
     * Intended for the "bot adds a comment" code path.
     *
     * @param postId     the post being commented on
     * @param botId      the bot authoring the comment
     * @param humanId    the human who owns the post (for cooldown tracking)
     * @param depthLevel the requested comment depth
     */
    void enforceAllBotCommentGuardrails(Long postId, Long botId, Long humanId, int depthLevel);
}
