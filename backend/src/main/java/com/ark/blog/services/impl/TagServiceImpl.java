package com.ark.blog.services.impl;

import com.ark.blog.domain.entities.Tag;
import com.ark.blog.repositories.TagRepository;
import com.ark.blog.services.TagService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private final TagRepository tagRepository;

    @Override
    public List<Tag> getTags() {
        return tagRepository.findAllWithPostCount();
    }

    @Transactional
    @Override
    public List<Tag> createTags(Set<String> tagNames) {
        List<Tag> existingTags = tagRepository.findByNameIn(tagNames);

        Set<String> existingTagNames = existingTags.stream().map(Tag::getName).collect(Collectors.toSet());
        List<Tag> newTags = tagNames.stream()
                .filter(tagName -> !existingTagNames.contains(tagName))
                .map(name -> Tag.builder()
                        .name(name)
                        .posts(new HashSet<>())
                        .build())
                .toList();
        List<Tag> savedTags = new ArrayList<>();

        if(!newTags.isEmpty()){
            savedTags = tagRepository.saveAll(newTags);
        }
        savedTags.addAll(existingTags);

        return savedTags;
    }

    @Override
    public void deleteTag(UUID id) {
        tagRepository.findById(id).ifPresent(tag -> {
            if(!tag.getPosts().isEmpty()){
                throw new IllegalArgumentException("Tag cannot be deleted because it has posts");
            }
            tagRepository.deleteById(id);
        });
    }

    @Override
    public Tag getTagById(UUID tagId) {
        return tagRepository.findById(tagId).orElseThrow(() ->
                new EntityNotFoundException("Tag not found with id: " + tagId));
    }


    @Override
    public List<Tag> getTagByIds(Set<UUID> ids) {
        List<Tag> foundTags = tagRepository.findAllById(ids);
        if (foundTags.size() != ids.size()){
            throw new IllegalArgumentException("Some tags not found for the given post IDs");
        }
        return foundTags;
    }


}
