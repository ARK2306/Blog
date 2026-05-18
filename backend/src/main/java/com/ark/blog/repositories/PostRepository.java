package com.ark.blog.repositories;

import com.ark.blog.domain.PostStatus;
import com.ark.blog.domain.entities.Category;
import com.ark.blog.domain.entities.Post;
import com.ark.blog.domain.entities.Tag;
import com.ark.blog.domain.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PostRepository extends JpaRepository<Post, UUID> {

    List<Post> findAllByStatusAndCategoryAndTagsContaining(PostStatus status, Category category, Tag tags);
    List<Post> findAllByStatusAndCategory( PostStatus status, Category category);
    List<Post> findAllByStatusAndTagsContaining(PostStatus status, Tag tag);
    List<Post> findAllByStatus(PostStatus status);
    List<Post> findAllByAuthorAndStatus( User author, PostStatus status);

}
