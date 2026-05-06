package com.grid07.dto;

import com.grid07.entity.Comment;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * Outbound representation of a {@link Comment}.
 */
@Data
@Builder
public class CommentResponse {

    private Long id;
    private Long postId;
    private String content;
    private int depthLevel;
    private Instant createdAt;

    /** Set when authored by a human. */
    private String authorUsername;

    /** Set when authored by a bot. */
    private String authorBotName;

    public static CommentResponse from(Comment comment) {
        String username = comment.getAuthorUser() != null ? comment.getAuthorUser().getUsername() : null;
        String botName  = comment.getAuthorBot()  != null ? comment.getAuthorBot().getName()      : null;

        return CommentResponse.builder()
                .id(comment.getId())
                .postId(comment.getPost().getId())
                .content(comment.getContent())
                .depthLevel(comment.getDepthLevel())
                .createdAt(comment.getCreatedAt())
                .authorUsername(username)
                .authorBotName(botName)
                .build();
    }
}
