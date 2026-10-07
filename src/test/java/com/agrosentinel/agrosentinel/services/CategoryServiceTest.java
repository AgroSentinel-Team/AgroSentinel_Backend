package com.agrosentinel.agrosentinel.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.agrosentinel.agrosentinel.dtos.CategoryRequest;
import com.agrosentinel.agrosentinel.models.Category;
import com.agrosentinel.agrosentinel.repositories.CategoryRepository;

class CategoryServiceTest {
    private final CategoryRepository categoryRepository = mock(CategoryRepository.class);
    private final CategoryService categoryService = new CategoryService(categoryRepository);

    @Test
    void rejectsCategoryNameThatAlreadyExistsRegardlessOfCaseOrWhitespace() {
        when(categoryRepository.existsByNameIgnoreCase("Filtros")).thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> categoryService.create(new CategoryRequest(" Filtros ", null, 90)));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void trimsCategoryNameBeforeSaving() {
        when(categoryRepository.existsByNameIgnoreCase("Filtros")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        categoryService.create(new CategoryRequest(" Filtros ", null, 90));

        verify(categoryRepository).save(org.mockito.ArgumentMatchers.argThat(
                category -> "Filtros".equals(category.getName())));
    }
}
