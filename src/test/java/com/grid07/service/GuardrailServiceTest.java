package com.grid07.service;

import com.grid07.config.RedisKeys;
import com.grid07.exception.GuardrailException;
import com.grid07.service.impl.GuardrailServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * Unit tests for {@link GuardrailServiceImpl}.
 *
 * <p>Redis interactions are mocked so these tests run without a live Redis
 * instance — they verify the business logic (threshold checks, exception
 * messages, key construction) rather than Redis itself.</p>
 */
@ExtendWith(MockitoExtension.class)
class GuardrailServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private GuardrailServiceImpl guardrailService;

    @BeforeEach
    void setup() {
        // lenient: some tests (vertical cap) don't call opsForValue() at all
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    // ─── Vertical Cap ────────────────────────────────────────────────────────

    @Test
    @DisplayName("Vertical cap passes when depth is exactly 20")
    void verticalCap_atBoundary_passes() {
        assertThatCode(() -> guardrailService.enforceVerticalCap(20))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Vertical cap throws GuardrailException when depth is 21")
    void verticalCap_oneOverBoundary_throws() {
        assertThatThrownBy(() -> guardrailService.enforceVerticalCap(21))
                .isInstanceOf(GuardrailException.class)
                .hasMessageContaining("21")
                .extracting("guardrailType")
                .isEqualTo("VERTICAL_CAP_EXCEEDED");
    }

    // ─── Cooldown Cap ────────────────────────────────────────────────────────

    @Test
    @DisplayName("Cooldown cap passes when no active cooldown key exists")
    void cooldownCap_noActiveCooldown_passes() {
        when(valueOperations.setIfAbsent(
                eq(RedisKeys.botHumanCooldown(1L, 10L)),
                eq("1"),
                any(Duration.class)
        )).thenReturn(true);

        assertThatCode(() -> guardrailService.enforceCooldownCap(1L, 10L))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Cooldown cap throws GuardrailException when cooldown key exists")
    void cooldownCap_activeCooldown_throws() {
        when(valueOperations.setIfAbsent(
                eq(RedisKeys.botHumanCooldown(1L, 10L)),
                eq("1"),
                any(Duration.class)
        )).thenReturn(false);

        // getExpire is called inside the exception message builder
        when(redisTemplate.getExpire(anyString())).thenReturn(300L);

        assertThatThrownBy(() -> guardrailService.enforceCooldownCap(1L, 10L))
                .isInstanceOf(GuardrailException.class)
                .extracting("guardrailType")
                .isEqualTo("COOLDOWN_CAP_ACTIVE");
    }

    // ─── Horizontal Cap ──────────────────────────────────────────────────────

    @Test
    @DisplayName("Horizontal cap passes when Lua script returns 1 (slot reserved)")
    void horizontalCap_slotAvailable_passes() {
        when(redisTemplate.execute(
                any(DefaultRedisScript.class),
                anyList(),
                anyString()
        )).thenReturn(1L);

        assertThatCode(() -> guardrailService.enforceHorizontalCap(42L))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Horizontal cap throws when Lua script returns 0 (cap exceeded)")
    void horizontalCap_capExceeded_throws() {
        when(redisTemplate.execute(
                any(DefaultRedisScript.class),
                anyList(),
                anyString()
        )).thenReturn(0L);

        assertThatThrownBy(() -> guardrailService.enforceHorizontalCap(42L))
                .isInstanceOf(GuardrailException.class)
                .hasMessageContaining("100")
                .extracting("guardrailType")
                .isEqualTo("HORIZONTAL_CAP_EXCEEDED");
    }
}
