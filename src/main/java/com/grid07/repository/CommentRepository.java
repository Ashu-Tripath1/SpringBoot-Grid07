package com.grid07.repository;

import com.grid07.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    /** All comments for a post, ordered shallowest-first then chronologically. */
    @Query("SELECT c FROM Comment c LEFT JOIN FETCH c.authorUser LEFT JOIN FETCH c.authorBot " +
           "WHERE c.post.id = :postId ORDER BY c.depthLevel ASC, c.createdAt ASC")
    List<Comment> findByPostIdOrdered(@Param("postId") Long postId);

    /** Count of comments at a specific depth level on a post (used for vertical cap checks). */
    long countByPostIdAndDepthLevel(Long postId, int depthLevel);
}
