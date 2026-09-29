package com.agrosentinel.agrosentinel.services;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.agrosentinel.agrosentinel.dtos.CategoryRequest;
import com.agrosentinel.agrosentinel.dtos.CategoryResponse;
import com.agrosentinel.agrosentinel.models.Category;
import com.agrosentinel.agrosentinel.repositories.CategoryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {
    private final CategoryRepository categoryRepository;

    public List<CategoryResponse> findActive() {
        return categoryRepository.findAll().stream()
                .filter(Category::isActive)
                .sorted(Comparator.comparing(Category::getName, String.CASE_INSENSITIVE_ORDER))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        Category category = new Category();
        category.setName(request.name().trim());
        category.setDescription(request.description());
        category.setActive(true);
        return toResponse(categoryRepository.save(category));
    }

    private CategoryResponse toResponse(Category category) {
        return new CategoryResponse(category.getId(), category.getName(), category.getDescription(), category.isActive());
    }
}