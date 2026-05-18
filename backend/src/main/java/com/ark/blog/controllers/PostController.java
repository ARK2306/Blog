package com.ark.blog.controllers;


import com.ark.blog.domain.CreatePostRequest;
import com.ark.blog.domain.dtos.CreatePostRequestDto;
import com.ark.blog.domain.dtos.PostDto;
import com.ark.blog.domain.entities.Post;
import com.ark.blog.domain.entities.User;
import com.ark.blog.mappers.PostMapper;
import com.ark.blog.services.PostService;
import com.ark.blog.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(path = "/api/v1/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final PostMapper postMapper;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<PostDto>> getAllPosts(
            @RequestParam(required = false)UUID categoryId,
            @RequestParam(required = false)UUID tagId
            ){
        List<Post> posts = postService.getAllPosts(categoryId, tagId);
        List<PostDto> postDtos = posts.stream().map(postMapper::toDto).toList();

        return ResponseEntity.ok(postDtos);

    }
    @GetMapping(path = "/drafts")
    public ResponseEntity<List<PostDto>> getDrafts(@RequestAttribute UUID userId){
        User loggedInUser = userService.getUserById(userId);
        List<Post> draftPosts = postService.getDraftPosts(loggedInUser);
        List<PostDto> postDtos = draftPosts.stream().map(postMapper::toDto).toList();
        return ResponseEntity.ok(postDtos);
    }

    @PostMapping
    public ResponseEntity<PostDto> createPost(
            @RequestBody CreatePostRequestDto createPostRequestDto,
            @RequestAttribute UUID userId
    ){
        User loggedInUser = userService.getUserById(userId);
        CreatePostRequest createPostRequest = postMapper.toCreatePostRequest(createPostRequestDto);
        Post createdPost = postService.createPost(loggedInUser, createPostRequest);
        PostDto createdPostDto = postMapper.toDto(createdPost);

        return new ResponseEntity<>(createdPostDto, HttpStatus.CREATED);

    }

    @PutMapping(path = "/{id}")
    public ResponseEntity<PostDto> updatePost(
            @PathVariable UUID id,
            @RequestBody CreatePostRequestDto updatePostRequestDto,
            @RequestAttribute UUID userId
    ){
        User loggedInUser = userService.getUserById(userId);
        CreatePostRequest updatePostRequest = postMapper.toCreatePostRequest(updatePostRequestDto);
        Post updatedPost = postService.updatePost(id, loggedInUser, updatePostRequest);
        return ResponseEntity.ok(postMapper.toDto(updatedPost));
    }

    @GetMapping(path = "/{id}")
    public ResponseEntity<PostDto> getPost(@PathVariable UUID id){

        Post post = postService.getPost(id);
        PostDto postDto = postMapper.toDto(post);
        return ResponseEntity.ok(postDto);
    }

    @DeleteMapping(path = "/{id}")
    public ResponseEntity<Void> deletePost(@PathVariable UUID id){
        postService.deletePost(id);
        return ResponseEntity.noContent().build();
    }

}
