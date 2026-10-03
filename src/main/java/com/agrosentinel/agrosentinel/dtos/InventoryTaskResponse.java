package com.agrosentinel.agrosentinel.dtos;

import java.time.Instant;

public record InventoryTaskResponse(
        Long id,
        String description,
        boolean completed,
        String createdBy,
        Instant createdAt,
        Instant completedAt) {
}
