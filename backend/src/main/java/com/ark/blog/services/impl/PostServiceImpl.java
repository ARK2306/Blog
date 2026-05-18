package com.ark.blog.services.impl;

import com.ark.blog.domain.CreatePostRequest;
import com.ark.blog.domain.PostStatus;
import com.ark.blog.domain.entities.Category;
import com.ark.blog.domain.entities.Post;
import com.ark.blog.domain.entities.Tag;
import com.ark.blog.domain.entities.User;
import com.ark.blog.repositories.PostRepository;
import com.ark.blog.services.CategoryService;
import com.ark.blog.services.PostService;
import com.ark.blog.services.TagService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final CategoryService categoryService;
    private final TagService tagService;

    @Override
    @Transactional(readOnly = true)
    public List<Post> getAllPosts(UUID categoryId, UUID tagId) {
        if(categoryId != null && tagId != null){
            Category category = categoryService.getCategoryById(categoryId);
            Tag tag = tagService.getTagById(tagId);
            postRepository.findAllByStatusAndCategoryAndTagsContaining(PostStatus.PUBLISHED, category, tag);
        }
        if(categoryId != null){
            Category category = categoryService.getCategoryById(categoryId);
            return postRepository.findAllByStatusAndCategory(PostStatus.PUBLISHED, category);
        }
        if(tagId != null){
            Tag tag = tagService.getTagById(tagId);
            return postRepository.findAllByStatusAndTagsContaining(PostStatus.PUBLISHED, tag);
        }
        return postRepository.findAllByStatus(PostStatus.PUBLISHED);
    }

    @Override
    public List<Post> getDraftPosts(User user) {
        return postRepository.findAllByAuthorAndStatus(user, PostStatus.DRAFT);

    }

    @Override
    @Transactional
    public Post createPost(User user, CreatePostRequest createPostRequest) {
        Post newPost = new Post();
        newPost.setTitle(createPostRequest.getTitle());
        newPost.setContent(createPostRequest.getContent());
        newPost.setStatus(createPostRequest.getStatus());
        newPost.setAuthor(user);
        newPost.setReadingTime(getReadingTime(createPostRequest.getContent()));

        Category category = categoryService.getCategoryById(createPostRequest.getCategoryId());
        newPost.setCategory(category);

        Set<UUID> tagIds = createPostRequest.getTagIds();
        List<Tag> tags = tagService.getTagByIds(tagIds);

        newPost.setTags(new HashSet<>(tags));

        return postRepository.save(newPost);
    }

    @Override
    @Transactional
    public Post updatePost(UUID postId, User user, CreatePostRequest updatePostRequest) {
        Post existingPost = postRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("Post does not exist with ID: " + postId));

        if (!existingPost.getAuthor().getId().equals(user.getId())) {
            throw new IllegalArgumentException("You are not authorized to update this post");
        }

        existingPost.setTitle(updatePostRequest.getTitle());
        existingPost.setContent(updatePostRequest.getContent());
        existingPost.setStatus(updatePostRequest.getStatus());
        existingPost.setReadingTime(getReadingTime(updatePostRequest.getContent()));

        Category category = categoryService.getCategoryById(updatePostRequest.getCategoryId());
        existingPost.setCategory(category);

        Set<UUID> tagIds = updatePostRequest.getTagIds();
        List<Tag> tags = tagService.getTagByIds(tagIds);
        existingPost.setTags(new HashSet<>(tags));

        return postRepository.save(existingPost);
    }

    @Override
    public Post getPost(UUID id) {
        return postRepository.findById(id).orElseThrow(()-> new EntityNotFoundException("Post doesn not exist with ID" + id));
    }

    @Override
    public void deletePost(UUID id) {
       Post post = getPost(id);
       postRepository.delete(post);
    }

    private Integer getReadingTime(String content){
        if(content == null || content.isBlank()){
            return 0;
        }
        return (int) Math.ceil(content.length() / 200.0);
    }

}
