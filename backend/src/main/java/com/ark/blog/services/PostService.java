package com.ark.blog.services;

import com.ark.blog.domain.CreatePostRequest;
import com.ark.blog.domain.entities.Post;
import com.ark.blog.domain.entities.User;

import java.util.List;
import java.util.UUID;

public interface PostService {
    List<Post> getAllPosts(UUID categoryId, UUID tagId);
    List<Post> getDraftPosts(User user);
    Post createPost(User user, CreatePostRequest createPostRequest);
    Post updatePost(UUID postId, User user, CreatePostRequest updatePostRequest);
    Post getPost(UUID id);
    void deletePost(UUID id);


}
