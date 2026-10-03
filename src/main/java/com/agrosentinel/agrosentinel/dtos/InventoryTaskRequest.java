package com.agrosentinel.agrosentinel.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InventoryTaskRequest(
        @NotBlank @Size(max = 240) String description) {
}
