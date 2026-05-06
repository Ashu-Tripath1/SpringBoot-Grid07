package com.grid07.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * Represents a top-level post on the platform.
 *
 * <p>A post can be authored by either a {@link User} or a {@link Bot}.
 * Exactly one of {@code authorUser} or {@code authorBot} must be non-null;
 * this invariant is enforced at the service layer rather than the DB level
 * to keep the schema clean.</p>
 */
@Entity
@Table(
    name = "posts",
    indexes = {
        @Index(name = "idx_posts_author_user",  columnList = "author_user_id"),
        @Index(name = "idx_posts_author_bot",   columnList = "author_bot_id"),
        @Index(name = "idx_posts_created_at",   columnList = "created_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"authorUser", "authorBot"})
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Human author — null if the post was created by a bot. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_user_id")
    private User authorUser;

    /** Bot author — null if the post was created by a human. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_bot_id")
    private Bot authorBot;

    @NotBlank
    @Size(min = 1, max = 5000)
    @Column(nullable = false, length = 5000)
    private String content;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
