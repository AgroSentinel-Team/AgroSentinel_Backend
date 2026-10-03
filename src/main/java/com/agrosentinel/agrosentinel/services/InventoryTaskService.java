package com.agrosentinel.agrosentinel.services;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.agrosentinel.agrosentinel.dtos.InventoryTaskRequest;
import com.agrosentinel.agrosentinel.dtos.InventoryTaskResponse;
import com.agrosentinel.agrosentinel.models.InventoryTask;
import com.agrosentinel.agrosentinel.repositories.InventoryTaskRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryTaskService {
    private final InventoryTaskRepository taskRepository;

    public List<InventoryTaskResponse> findAll() {
        return taskRepository.findAllByOrderByCompletedAscCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public InventoryTaskResponse create(InventoryTaskRequest request, String createdBy) {
        InventoryTask task = new InventoryTask();
        task.setDescription(request.description().trim());
        task.setCreatedBy(createdBy);
        return toResponse(taskRepository.save(task));
    }

    @Transactional
    public InventoryTaskResponse setCompleted(Long id, boolean completed) {
        InventoryTask task = taskRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tarea no encontrada"));
        task.setCompleted(completed);
        return toResponse(taskRepository.save(task));
    }

    @Transactional
    public void delete(Long id) {
        if (!taskRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tarea no encontrada");
        }
        taskRepository.deleteById(id);
    }

    private InventoryTaskResponse toResponse(InventoryTask task) {
        return new InventoryTaskResponse(
                task.getId(),
                task.getDescription(),
                task.isCompleted(),
                task.getCreatedBy(),
                task.getCreatedAt(),
                task.getCompletedAt());
    }
}
