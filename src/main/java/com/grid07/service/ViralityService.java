package com.grid07.service;

/**
 * Manages the real-time virality score for posts.
 *
 * <p>Scores are maintained exclusively in Redis so they can be updated
 * atomically without a round-trip to PostgreSQL on every interaction.</p>
 */
public interface ViralityService {

    /**
     * Increments the virality score for the given post based on the interaction type.
     *
     * <ul>
     *   <li>Bot Reply    → +1  point</li>
     *   <li>Human Like   → +20 points</li>
     *   <li>Human Comment→ +50 points</li>
     * </ul>
     *
     * @param postId        the post whose score should be updated
     * @param interactionType the type of interaction that just occurred
     */
    void recordInteraction(Long postId, InteractionType interactionType);

    /**
     * Returns the current virality score for a post.
     * Returns {@code 0} if no interactions have been recorded yet.
     *
     * @param postId the post to query
     * @return the current virality score
     */
    long getScore(Long postId);

    /** The types of interactions the scoring engine recognises. */
    enum InteractionType {
        BOT_REPLY(1),
        HUMAN_LIKE(20),
        HUMAN_COMMENT(50);

        private final int points;

        InteractionType(int points) {
            this.points = points;
        }

        public int getPoints() {
            return points;
        }
    }
}
