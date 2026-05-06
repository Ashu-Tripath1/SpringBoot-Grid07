package com.grid07.service;

import com.grid07.config.RedisKeys;
import com.grid07.service.impl.ViralityServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * Unit tests for {@link ViralityServiceImpl}.
 *
 * <p>Verifies that each {@link ViralityService.InteractionType} increments
 * the correct Redis key by the correct point value, and that {@code getScore}
 * returns {@code 0} gracefully when no key exists yet.</p>
 */
@ExtendWith(MockitoExtension.class)
class ViralityServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private ViralityServiceImpl viralityService;

    @BeforeEach
    void setup() {
        // lenient: the PRD-points assertion test doesn't call opsForValue()
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    // ─── recordInteraction ────────────────────────────────────────────────────

    @Test
    @DisplayName("Bot reply increments virality score by +1")
    void recordInteraction_botReply_incrementsByOne() {
        String key = RedisKeys.viralityScore(1L);

        viralityService.recordInteraction(1L, ViralityService.InteractionType.BOT_REPLY);

        verify(valueOperations).increment(eq(key), eq((long) 1));
    }

    @Test
    @DisplayName("Human like increments virality score by +20")
    void recordInteraction_humanLike_incrementsByTwenty() {
        String key = RedisKeys.viralityScore(1L);

        viralityService.recordInteraction(1L, ViralityService.InteractionType.HUMAN_LIKE);

        verify(valueOperations).increment(eq(key), eq((long) 20));
    }

    @Test
    @DisplayName("Human comment increments virality score by +50")
    void recordInteraction_humanComment_incrementsByFifty() {
        String key = RedisKeys.viralityScore(1L);

        viralityService.recordInteraction(1L, ViralityService.InteractionType.HUMAN_COMMENT);

        verify(valueOperations).increment(eq(key), eq((long) 50));
    }

    // ─── getScore ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getScore returns parsed long when key exists")
    void getScore_keyExists_returnsParsedValue() {
        String key = RedisKeys.viralityScore(5L);
        when(valueOperations.get(key)).thenReturn("142");

        long score = viralityService.getScore(5L);

        assertThat(score).isEqualTo(142L);
    }

    @Test
    @DisplayName("getScore returns 0 when key does not exist in Redis")
    void getScore_keyMissing_returnsZero() {
        when(valueOperations.get(anyString())).thenReturn(null);

        long score = viralityService.getScore(99L);

        assertThat(score).isZero();
    }

    @Test
    @DisplayName("Each interaction type has the exact points mandated by the PRD")
    void interactionTypePoints_matchPrd() {
        assertThat(ViralityService.InteractionType.BOT_REPLY.getPoints()).isEqualTo(1);
        assertThat(ViralityService.InteractionType.HUMAN_LIKE.getPoints()).isEqualTo(20);
        assertThat(ViralityService.InteractionType.HUMAN_COMMENT.getPoints()).isEqualTo(50);
    }
}
