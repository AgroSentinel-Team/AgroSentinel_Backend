package com.agrosentinel.agrosentinel.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.agrosentinel.agrosentinel.dtos.InventoryTaskRequest;
import com.agrosentinel.agrosentinel.dtos.InventoryTaskResponse;
import com.agrosentinel.agrosentinel.services.InventoryTaskService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/inventory/tasks")
@RequiredArgsConstructor
public class InventoryTaskController {
    private final InventoryTaskService taskService;

    @GetMapping
    public List<InventoryTaskResponse> findAll() {
        return taskService.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryTaskResponse create(
            @Valid @RequestBody InventoryTaskRequest request,
            Authentication authentication) {
        return taskService.create(request, authentication.getName());
    }

    @PatchMapping("/{id}")
    public InventoryTaskResponse setCompleted(
            @PathVariable Long id,
            @RequestParam boolean completed) {
        return taskService.setCompleted(id, completed);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        taskService.delete(id);
    }
}
