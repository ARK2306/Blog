package com.ark.blog.mappers;

import com.ark.blog.domain.PostStatus;
import com.ark.blog.domain.dtos.CategoryDto;
import com.ark.blog.domain.dtos.CreateCategoryRequest;
import com.ark.blog.domain.entities.Category;
import com.ark.blog.domain.entities.Post;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CategoryMapper {

    @Mapping(target ="postCount",source = "post",qualifiedByName = "calculatePostCount")
    CategoryDto toDto(Category category);

    Category toEntity(CreateCategoryRequest createCategoryRequest);

    @Named( "calculatePostCount")
    default long calculatePostCount(List<Post> posts){
        if(posts == null){
            return 0;
        }
       return posts.stream()
                .filter(post -> PostStatus.PUBLISHED.equals(post.getStatus()))
                .count();
    }

}
