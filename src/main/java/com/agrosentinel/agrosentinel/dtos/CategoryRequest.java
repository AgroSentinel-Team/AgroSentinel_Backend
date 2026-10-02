package com.agrosentinel.agrosentinel.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

public record CategoryRequest(
        @NotBlank String name,
        String description,
        @Min(1) @Max(3650) Integer maintenanceFrequencyDays) {
}