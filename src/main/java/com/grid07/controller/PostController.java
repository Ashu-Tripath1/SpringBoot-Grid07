package com.grid07.controller;

import com.grid07.dto.*;
import com.grid07.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller exposing the post and comment endpoints.
 *
 * <p>This controller is intentionally thin — it delegates all business logic
 * to {@link PostService}.  Input validation is handled by Bean Validation
 * annotations on the request DTOs, with errors caught by
 * {@link com.grid07.exception.GlobalExceptionHandler}.</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    /**
     * Creates a new post.
     *
     * @param request must contain exactly one of {@code userId} or {@code botId},
     *                plus a non-blank {@code content} string
     * @return {@code 201 Created} with the persisted post body
     */
    @PostMapping
    public ResponseEntity<PostResponse> createPost(@Valid @RequestBody CreatePostRequest request) {
        PostResponse response = postService.createPost(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieves a single post by ID, including its current virality score.
     *
     * @param postId path variable identifying the post
     * @return {@code 200 OK} with the post body
     */
    @GetMapping("/{postId}")
    public ResponseEntity<PostResponse> getPost(@PathVariable Long postId) {
        return ResponseEntity.ok(postService.getPost(postId));
    }

    /**
     * Adds a comment to a post, enforcing all Redis guardrails when the
     * commenter is a bot.
     *
     * @param postId  the target post
     * @param request comment payload — must specify {@code userId} or {@code botId}
     * @return {@code 201 Created} with the persisted comment body,
     *         or {@code 429 Too Many Requests} if a guardrail blocks the request
     */
    @PostMapping("/{postId}/comments")
    public ResponseEntity<CommentResponse> addComment(
            @PathVariable Long postId,
            @Valid @RequestBody CreateCommentRequest request) {

        CommentResponse response = postService.addComment(postId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Records a human "like" on a post and increments its virality score by 20.
     *
     * @param postId  the post being liked
     * @param request must contain a valid {@code userId}
     * @return {@code 200 OK} with the updated post (including new virality score)
     */
    @PostMapping("/{postId}/like")
    public ResponseEntity<PostResponse> likePost(
            @PathVariable Long postId,
            @Valid @RequestBody LikePostRequest request) {

        PostResponse response = postService.likePost(postId, request);
        return ResponseEntity.ok(response);
    }
}
