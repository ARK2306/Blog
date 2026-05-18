package com.ark.blog.domain.dtos;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateTagsRequest {

    @NotEmpty(message = "At least one tag name is required")
    @Size(max=10,message = "You can only add up to 10 tags")
    private Set<
            @Size(min=2,max=30,message = "Tag name must be between {min} and {max} characters")
            @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Tag name can only contain letters, numbers, underscores, and hyphens")
            String> names;
}
