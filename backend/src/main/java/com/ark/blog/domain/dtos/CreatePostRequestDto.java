package com.ark.blog.domain.dtos;

import com.ark.blog.domain.PostStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreatePostRequestDto {
    @NotBlank(message = "Title cannot be blank")
    @Size(min = 3, max = 255, message = "Title must be between {min} and {max} characters")
    private String title;

    @NotBlank(message = "Content cannot be blank")
    @Size(min = 10, message = "Content must be at least {min} characters")
    private String content;

    @NotBlank(message = "Category cannot be blank")
    private UUID categoryId;

    @Builder.Default
    @Size(max = 10, message = "Can only have {max} tags")
    private Set<UUID> tagIds = new HashSet<>();

    @NotBlank(message = "Status cannot be blank")
    private PostStatus status;


}
