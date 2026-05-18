package com.ark.blog.mappers;

import com.ark.blog.domain.CreatePostRequest;
import com.ark.blog.domain.dtos.CreatePostRequestDto;
import com.ark.blog.domain.dtos.PostDto;
import com.ark.blog.domain.entities.Post;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = {CategoryMapper.class, TagMapper.class})
public interface  PostMapper {


    @Mapping(target ="author", source = "author")
    @Mapping(target = "category", source = "category")
    @Mapping(target = "tags", source = "tags")
    PostDto toDto(Post post);

    CreatePostRequest toCreatePostRequest(CreatePostRequestDto createPostRequestDto);
}
