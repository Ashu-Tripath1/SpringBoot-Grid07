package com.grid07.service.impl;

import com.grid07.dto.*;
import com.grid07.entity.*;
import com.grid07.exception.InvalidRequestException;
import com.grid07.exception.ResourceNotFoundException;
import com.grid07.repository.*;
import com.grid07.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates all post-related operations, coordinating between PostgreSQL
 * for durable storage and Redis for guardrails, scoring, and notifications.
 *
 * <h2>Data Integrity Contract</h2>
 *
 * <p>Redis guardrails are checked <em>before</em> the JPA transaction is
 * opened. If a guardrail blocks the request, no DB write is attempted.
 * The JPA write itself happens inside a {@link Transactional} boundary;
 * if the DB commit fails for any reason, the Redis counters and scores
 * may be slightly ahead of the DB state — an acceptable trade-off given
 * that Redis is the gatekeeper (not the source of truth).</p>
 *
 * <p>In a real production system the horizontal-cap rollback in the Lua
 * script handles the symmetrical case: if the DB commit fails after the
 * Redis slot was reserved, a compensating DECR should be called. That
 * compensation is left as a TODO for the production hardening phase and
 * would typically be implemented with Spring's transaction synchronisation
 * API or an outbox pattern.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostRepository    postRepository;
    private final CommentRepository commentRepository;
    private final UserRepository    userRepository;
    private final BotRepository     botRepository;

    private final ViralityService    viralityService;
    private final GuardrailService   guardrailService;
    private final NotificationService notificationService;

    // ─── Create Post ─────────────────────────────────────────────────────────

    @Override
    @Transactional
    public PostResponse createPost(CreatePostRequest request) {
        validateSingleAuthor(request.getUserId(), request.getBotId());

        Post.PostBuilder builder = Post.builder().content(request.getContent());

        if (request.getUserId() != null) {
            User author = findUserOrThrow(request.getUserId());
            builder.authorUser(author);
        } else {
            Bot author = findBotOrThrow(request.getBotId());
            builder.authorBot(author);
        }

        Post saved = postRepository.save(builder.build());
        log.info("Post created — id={}, authorType={}", saved.getId(),
                request.getUserId() != null ? "USER" : "BOT");

        return PostResponse.from(saved, 0L);
    }

    // ─── Add Comment ─────────────────────────────────────────────────────────

    @Override
    @Transactional
    public CommentResponse addComment(Long postId, CreateCommentRequest request) {
        validateSingleAuthor(request.getUserId(), request.getBotId());

        Post post = findPostOrThrow(postId);
        boolean isBotComment = request.getBotId() != null;

        if (isBotComment) {
            enforceGuardrailsForBotComment(postId, request, post);
        }

        Comment.CommentBuilder builder = Comment.builder()
                .post(post)
                .content(request.getContent())
                .depthLevel(request.getDepthLevel());

        if (!isBotComment) {
            User author = findUserOrThrow(request.getUserId());
            builder.authorUser(author);
        } else {
            Bot author = findBotOrThrow(request.getBotId());
            builder.authorBot(author);
        }

        Comment saved = commentRepository.save(builder.build());

        // Update virality score and trigger notifications after successful DB commit
        if (isBotComment) {
            viralityService.recordInteraction(postId, ViralityService.InteractionType.BOT_REPLY);
        } else {
            viralityService.recordInteraction(postId, ViralityService.InteractionType.HUMAN_COMMENT);

            // A human comment on a bot's post does not trigger a bot notification; skip.
        }

        // If a bot commented on a human's post, notify the post owner
        if (isBotComment && post.getAuthorUser() != null) {
            Bot bot = saved.getAuthorBot();
            String notificationText = String.format("Bot %s replied to your post", bot.getName());
            notificationService.handleBotInteractionNotification(post.getAuthorUser().getId(), notificationText);
        }

        log.info("Comment added — id={}, post={}, depth={}, authorType={}",
                saved.getId(), postId, saved.getDepthLevel(), isBotComment ? "BOT" : "USER");

        return CommentResponse.from(saved);
    }

    // ─── Like Post ───────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PostResponse likePost(Long postId, LikePostRequest request) {
        // Verify the user and post both exist before touching Redis
        findUserOrThrow(request.getUserId());
        Post post = findPostOrThrow(postId);

        viralityService.recordInteraction(postId, ViralityService.InteractionType.HUMAN_LIKE);
        long newScore = viralityService.getScore(postId);

        log.info("Post liked — postId={}, userId={}, newViralityScore={}", postId, request.getUserId(), newScore);

        return PostResponse.from(post, newScore);
    }

    // ─── Get Post ────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PostResponse getPost(Long postId) {
        Post post = findPostOrThrow(postId);
        long score = viralityService.getScore(postId);
        return PostResponse.from(post, score);
    }

    // ─── Private helpers ─────────────────────────────────────────────────────

    /**
     * Runs all three guardrails for an incoming bot comment.
     * The vertical cap check happens first (cheapest); the Lua horizontal
     * cap check happens last (most expensive, requires network round-trip).
     */
    private void enforceGuardrailsForBotComment(Long postId, CreateCommentRequest request, Post post) {
        // Determine the human whose post is being commented on for the cooldown key
        Long targetUserId = resolveTargetUserId(request, post);

        if (targetUserId == null) {
            // Bot commenting on another bot's post — only horizontal and vertical caps apply
            guardrailService.enforceVerticalCap(request.getDepthLevel());
            guardrailService.enforceHorizontalCap(postId);
        } else {
            guardrailService.enforceAllBotCommentGuardrails(
                    postId, request.getBotId(), targetUserId, request.getDepthLevel());
        }
    }

    /**
     * Determines the human target for the cooldown cap.
     * Uses {@code request.targetUserId} if provided; otherwise falls back to
     * the post's human author.
     */
    private Long resolveTargetUserId(CreateCommentRequest request, Post post) {
        if (request.getTargetUserId() != null) {
            return request.getTargetUserId();
        }
        return post.getAuthorUser() != null ? post.getAuthorUser().getId() : null;
    }

    private void validateSingleAuthor(Long userId, Long botId) {
        if (userId == null && botId == null) {
            throw new InvalidRequestException("Either userId or botId must be provided.");
        }
        if (userId != null && botId != null) {
            throw new InvalidRequestException("Only one of userId or botId may be provided, not both.");
        }
    }

    private User findUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }

    private Bot findBotOrThrow(Long botId) {
        return botRepository.findById(botId)
                .orElseThrow(() -> new ResourceNotFoundException("Bot", botId));
    }

    private Post findPostOrThrow(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", postId));
    }
}
