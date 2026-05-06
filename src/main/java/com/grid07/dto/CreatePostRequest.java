package com.grid07.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

// ─── Request DTOs ──────────────────────────────────────────────────────────

/**
 * Payload for POST /api/posts
 * Exactly one of {@code userId} or {@code botId} must be provided.
 */
@Data
public class CreatePostRequest {

    /** ID of the human author. Mutually exclusive with {@code botId}. */
    private Long userId;

    /** ID of the bot author. Mutually exclusive with {@code userId}. */
    private Long botId;

    @NotBlank(message = "Post content must not be blank")
    @Size(min = 1, max = 5000, message = "Post content must be between 1 and 5000 characters")
    private String content;
}
