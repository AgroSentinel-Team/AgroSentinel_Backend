package com.agrosentinel.agrosentinel.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.agrosentinel.agrosentinel.dtos.InventoryTaskRequest;
import com.agrosentinel.agrosentinel.dtos.InventoryTaskResponse;
import com.agrosentinel.agrosentinel.models.InventoryTask;
import com.agrosentinel.agrosentinel.repositories.InventoryTaskRepository;

class InventoryTaskServiceTest {
    private InventoryTaskRepository repository;
    private InventoryTaskService service;

    @BeforeEach
    void setUp() {
        repository = mock(InventoryTaskRepository.class);
        service = new InventoryTaskService(repository);
    }

    @Test
    void createTrimsDescriptionAndRecordsCreator() {
        when(repository.save(any(InventoryTask.class))).thenAnswer(invocation -> {
            InventoryTask task = invocation.getArgument(0);
            task.setId(8L);
            task.setCreatedAt(Instant.parse("2026-10-03T17:00:00Z"));
            return task;
        });

        InventoryTaskResponse response = service.create(
                new InventoryTaskRequest("  Revisar estante A  "),
                "warehouse@agrosentinel.test");

        assertEquals("Revisar estante A", response.description());
        assertEquals("warehouse@agrosentinel.test", response.createdBy());
        assertFalse(response.completed());
    }

    @Test
    void setCompletedPersistsCompletionState() {
        InventoryTask task = new InventoryTask();
        task.setId(8L);
        task.setDescription("Revisar estante A");
        task.setCreatedBy("warehouse@agrosentinel.test");
        when(repository.findById(8L)).thenReturn(Optional.of(task));
        when(repository.save(task)).thenReturn(task);

        InventoryTaskResponse response = service.setCompleted(8L, true);

        assertTrue(response.completed());
        verify(repository).save(task);
    }

    @Test
    void deleteRemovesExistingTask() {
        when(repository.existsById(8L)).thenReturn(true);

        assertDoesNotThrow(() -> service.delete(8L));

        verify(repository).deleteById(8L);
    }
}
