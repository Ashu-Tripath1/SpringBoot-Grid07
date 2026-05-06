package com.grid07.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * Represents a comment on a {@link Post}.
 *
 * <p>Comments are arranged in a tree via the {@code depthLevel} field.
 * A top-level reply has {@code depthLevel = 1}; each nested reply increments
 * the depth by one.  The vertical cap guardrail rejects comments whose
 * {@code depthLevel} would exceed 20.</p>
 */
@Entity
@Table(
    name = "comments",
    indexes = {
        @Index(name = "idx_comments_post_id",    columnList = "post_id"),
        @Index(name = "idx_comments_author_user", columnList = "author_user_id"),
        @Index(name = "idx_comments_author_bot",  columnList = "author_bot_id"),
        @Index(name = "idx_comments_created_at",  columnList = "created_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"post", "authorUser", "authorBot"})
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    /** Human author — null if authored by a bot. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_user_id")
    private User authorUser;

    /** Bot author — null if authored by a human. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_bot_id")
    private Bot authorBot;

    @NotBlank
    @Size(min = 1, max = 2000)
    @Column(nullable = false, length = 2000)
    private String content;

    /**
     * Nesting depth of this comment within the post's comment tree.
     * Top-level replies are depth 1; must not exceed 20.
     */
    @Min(1)
    @Column(name = "depth_level", nullable = false)
    private int depthLevel;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
