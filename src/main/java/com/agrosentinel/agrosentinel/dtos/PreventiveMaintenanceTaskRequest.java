package com.agrosentinel.agrosentinel.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PreventiveMaintenanceTaskRequest(
        @NotBlank @Size(max = 200) String description,
        boolean completed) {
}
