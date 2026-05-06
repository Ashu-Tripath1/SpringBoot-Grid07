package com.grid07.dto;

import com.grid07.entity.Post;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * Outbound representation of a {@link Post}.
 * We intentionally exclude lazy-loaded collections to prevent accidental N+1 queries.
 */
@Data
@Builder
public class PostResponse {

    private Long id;
    private String content;
    private Instant createdAt;

    /** Populated when the post was created by a human. */
    private String authorUsername;

    /** Populated when the post was created by a bot. */
    private String authorBotName;

    /** Current virality score pulled from Redis. */
    private long viralityScore;

    public static PostResponse from(Post post, long viralityScore) {
        String username = post.getAuthorUser() != null ? post.getAuthorUser().getUsername() : null;
        String botName  = post.getAuthorBot()  != null ? post.getAuthorBot().getName()      : null;

        return PostResponse.builder()
                .id(post.getId())
                .content(post.getContent())
                .createdAt(post.getCreatedAt())
                .authorUsername(username)
                .authorBotName(botName)
                .viralityScore(viralityScore)
                .build();
    }
}
