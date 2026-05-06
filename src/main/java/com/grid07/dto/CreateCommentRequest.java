package com.grid07.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Payload for POST /api/posts/{postId}/comments.
 *
 * <p>Exactly one of {@code userId} or {@code botId} must be supplied.
 * If neither (or both) is provided, the service layer will reject the request
 * with a {@code 400 Bad Request}.</p>
 */
@Data
public class CreateCommentRequest {

    /** ID of the human commenter. Mutually exclusive with {@code botId}. */
    private Long userId;

    /** ID of the bot commenter. Mutually exclusive with {@code userId}. */
    private Long botId;

    /**
     * ID of the human whose post/comment is being replied to.
     * Required when {@code botId} is present so the cooldown key
     * {@code cooldown:bot_{id}:human_{id}} can be constructed correctly.
     */
    private Long targetUserId;

    @NotBlank(message = "Comment content must not be blank")
    @Size(min = 1, max = 2000, message = "Comment must be between 1 and 2000 characters")
    private String content;

    /**
     * Nesting depth of this comment.  Top-level replies must supply {@code 1};
     * each nested reply increments by one.  Must not exceed 20.
     */
    @Min(value = 1, message = "Depth level must be at least 1")
    private int depthLevel = 1;
}
