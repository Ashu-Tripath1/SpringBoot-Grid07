package com.grid07.service;

import com.grid07.dto.*;

/**
 * Business logic for creating and interacting with posts.
 */
public interface PostService {

    /**
     * Creates a new post authored by either a human user or a bot.
     *
     * @param request the creation payload
     * @return the persisted post as a response DTO
     */
    PostResponse createPost(CreatePostRequest request);

    /**
     * Adds a comment to a post, enforcing all applicable Redis guardrails
     * if the commenter is a bot.
     *
     * @param postId  the target post
     * @param request the comment payload
     * @return the persisted comment as a response DTO
     */
    CommentResponse addComment(Long postId, CreateCommentRequest request);

    /**
     * Records a human like on a post and updates the virality score.
     *
     * @param postId  the post being liked
     * @param request the like payload carrying the user's ID
     * @return the updated post response including the new virality score
     */
    PostResponse likePost(Long postId, LikePostRequest request);

    /**
     * Retrieves a post by ID, with its current virality score from Redis.
     *
     * @param postId the post to retrieve
     * @return the post response DTO
     */
    PostResponse getPost(Long postId);
}
