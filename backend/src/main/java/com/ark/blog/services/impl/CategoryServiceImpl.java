package com.ark.blog.services.impl;

import com.ark.blog.domain.entities.Category;
import com.ark.blog.repositories.CategoryRepository;
import com.ark.blog.services.CategoryService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    @Override
    public List<Category> listCategories() {
        return categoryRepository.findAllWithPostCount();
    }

    @Override
    @Transactional
    public Category createCategory(Category category) {
        if(categoryRepository.existsByNameIgnoreCase(category.getName())){
            throw new IllegalArgumentException("Category with name '" + category.getName() + "' already exists");
        }
        return categoryRepository.save(category);
    }

    @Override
    public void deleteCategory(UUID id) {
        Optional<Category> category = categoryRepository.findById(id);
        if(category.isPresent()){
            if (!category.get().getPost().isEmpty()){
                throw new IllegalArgumentException("Category cannot be deleted because it has posts");
            }
        }
        categoryRepository.deleteById(id);
    }

    @Override
    public Category getCategoryById(UUID id) {
        return categoryRepository.findById(id).orElseThrow(() ->
                new EntityNotFoundException("Category not found with id: " + id + ""));
    }
}
