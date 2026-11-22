package com.agrosentinel.agrosentinel.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.agrosentinel.agrosentinel.dtos.CategoryRequest;
import com.agrosentinel.agrosentinel.dtos.CategoryResponse;
import com.agrosentinel.agrosentinel.dtos.SpecFieldResponse;
import com.agrosentinel.agrosentinel.dtos.SpecPresetResponse;
import com.agrosentinel.agrosentinel.dtos.SpecTemplateRequest;
import com.agrosentinel.agrosentinel.services.AssetSpecificationService;
import com.agrosentinel.agrosentinel.services.CategoryService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/categories")
@CrossOrigin(origins = "http://localhost:5173")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService categoryService;
    private final AssetSpecificationService specificationService;

    @GetMapping
    public List<CategoryResponse> findActive() {
        return categoryService.findActive();
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.create(request));
    }

    /** Plantillas sugeridas por familia de equipo (tractores, labranza, siembra...). */
    @GetMapping("/spec-presets")
    public List<SpecPresetResponse> specPresets() {
        return specificationService.presets();
    }

    @GetMapping("/{id}/spec-template")
    public List<SpecFieldResponse> getSpecTemplate(@PathVariable Long id) {
        return specificationService.getTemplate(id);
    }

    @PutMapping("/{id}/spec-template")
    public List<SpecFieldResponse> replaceSpecTemplate(@PathVariable Long id,
            @Valid @RequestBody SpecTemplateRequest request) {
        return specificationService.replaceTemplate(id, request.fields());
    }
}