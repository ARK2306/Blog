package com.ark.blog.domain.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCategoryRequest {

    @NotBlank(message = "Name cannot be blank")
    @Size(min = 2,max = 50,message = "Category name must be between {min} and {max} characters")
    @Pattern(regexp = "^[a-zA-Z0-9\\s]+$", message = "Category name can only contain letters, numbers, and spaces")
    private String name;

}
