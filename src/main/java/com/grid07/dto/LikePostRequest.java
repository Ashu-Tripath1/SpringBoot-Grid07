package com.grid07.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Payload for POST /api/posts/{postId}/like.
 * Only human users can like posts; a bot ID would be invalid here.
 */
@Data
public class LikePostRequest {

    @NotNull(message = "userId is required to like a post")
    private Long userId;
}
