package com.agrosentinel.agrosentinel.dtos;

import com.agrosentinel.agrosentinel.models.FailurePriority;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FailureReportRequest(
        @NotNull Long assetId,
        @NotBlank @Size(max = 120) String tipoFalla,
        @NotNull FailurePriority prioridad,
        @NotBlank @Size(max = 5000) String descripcion,
        Long groupId,
        Long assignedTechnicianId) {
}
