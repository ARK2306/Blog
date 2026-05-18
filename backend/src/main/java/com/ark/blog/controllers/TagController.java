package com.ark.blog.controllers;

import com.ark.blog.domain.dtos.CreateTagsRequest;
import com.ark.blog.domain.dtos.TagDto;
import com.ark.blog.domain.entities.Tag;
import com.ark.blog.mappers.TagMapper;
import com.ark.blog.services.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(path = "/api/v1/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;
    private final TagMapper tagMapper;

    @GetMapping
    public ResponseEntity<List<TagDto>> getAllTags(){
        return ResponseEntity.ok(tagService.getTags().stream()
                .map(tagMapper::toTagResponse)
                .toList());
    }

    @PostMapping
    public ResponseEntity<List<TagDto>> createTags(@RequestBody CreateTagsRequest createTagsRequest){

        List<Tag> savedTags = tagService.createTags(createTagsRequest.getNames());
        List<TagDto> createdTagResponses = savedTags.stream().map(tagMapper::toTagResponse).toList();

        return new ResponseEntity<>(createdTagResponses, HttpStatus.CREATED);

    }

    @DeleteMapping(path="/{id}")
    public ResponseEntity<Void> deleteTag(@PathVariable UUID id){

        tagService.deleteTag(id);
        return ResponseEntity.noContent().build();

    }


}
