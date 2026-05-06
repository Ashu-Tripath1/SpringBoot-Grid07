package com.grid07.repository;

import com.grid07.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    /**
     * Fetch posts with their authors eagerly to avoid N+1 queries
     * in list views.
     */
    @Query("SELECT p FROM Post p LEFT JOIN FETCH p.authorUser LEFT JOIN FETCH p.authorBot ORDER BY p.createdAt DESC")
    Page<Post> findAllWithAuthors(Pageable pageable);
}
